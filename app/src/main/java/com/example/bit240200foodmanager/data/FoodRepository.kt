package com.example.bit240200foodmanager.data

import android.content.Context
import androidx.room.Room
import com.example.bit240200foodmanager.BuildConfig
import com.example.bit240200foodmanager.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

/** Room is the single source of truth; API sync is best-effort/idempotent by client UUID. */
object FoodRepository {
    private lateinit var db: FoodDatabase
    private val lock = Mutex()
    private val api: FoodApi by lazy {
        Retrofit.Builder().baseUrl(BuildConfig.MOCK_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build().create(FoodApi::class.java)
    }
    fun initialized() = ::db.isInitialized
    fun configure(context: Context) {
        if (!initialized()) db = Room.databaseBuilder(context.applicationContext, FoodDatabase::class.java,
            "BIT240200_FoodManager.db").build()
    }
    private val dao: FoodDao get() = db.foodDao()
    fun syncConfigured() = BuildConfig.MOCK_API_BASE_URL.startsWith("https://") &&
        !BuildConfig.MOCK_API_BASE_URL.contains("example.mockapi.io")

    suspend fun seedOnce(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("BIT240200_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return@withContext
        // Fixed seed IDs persist across retries and prevent duplicate POST requests.
        val c1 = "BIT240200_category_1"; val c2 = "BIT240200_category_2"; val c3 = "BIT240200_category_3"
        if (dao.allCategoriesRaw().isEmpty()) {
            dao.saveCategory(CategoryEntity(c1, "Main dishes", "Rice, noodles, everyday meals"))
            dao.saveCategory(CategoryEntity(c2, "Fast food", "Burger, pizza, snacks"))
            dao.saveCategory(CategoryEntity(c3, "Drinks", "Beverages and fruit drinks"))
        }
        if (dao.allDishesRaw().isEmpty()) {
            val samples = listOf(
                Triple("Fried rice", c1, "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=700"),
                Triple("Pho noodle soup", c1, "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=700"),
                Triple("Beef burger", c2, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=700"),
                Triple("Pizza", c2, "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=700"),
                Triple("Orange juice", c3, "https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=700"),
                Triple("Iced coffee", c3, "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=700")
            )
            samples.forEachIndexed { index, (name, category, photo) ->
                dao.saveDish(DishEntity("BIT240200_dish_${index+1}", name,
                    (index+2) * 15000.0, if (index % 2 == 0) 10 else 25,
                    index != 5, "Freshly prepared ${name.lowercase()}", category,
                    imagesToJson(listOf(DishImage(photo, true),
                        DishImage("https://picsum.photos/seed/food${index+1}/700/450", false)))))
            }
        }
        prefs.edit().putBoolean("seeded", true).apply()
    }
    suspend fun pendingCount(): Int = dao.allCategoriesRaw().count { it.pending.isNotEmpty() } +
        dao.allDishesRaw().count { it.pending.isNotEmpty() }
    suspend fun categories(): List<CategoryEntity> = dao.allCategoriesRaw().filterNot { it.deleted }.sortedBy { it.name.lowercase() }
    suspend fun dishes(): List<DishEntity> = dao.allDishesRaw().filterNot { it.deleted }
    suspend fun category(uid: String) = dao.category(uid)?.takeUnless { it.deleted }
    suspend fun dish(uid: String) = dao.dish(uid)?.takeUnless { it.deleted }

    suspend fun saveCategory(uid: String?, name: String, description: String) {
        val current = uid?.let { dao.category(it) }
        val model = current?.copy(name = name.trim(), description = description.trim(), pending =
            if (current.remoteId == null) "CREATE" else "UPDATE")
            ?: CategoryEntity(UUID.randomUUID().toString(), name.trim(), description.trim())
        dao.saveCategory(model)
    }
    suspend fun deleteCategory(uid: String): Boolean {
        if (dao.allDishesRaw().any { !it.deleted && it.categoryId == uid }) return false
        val old = dao.category(uid) ?: return true
        if (old.remoteId == null) dao.removeCategory(uid)
        else dao.saveCategory(old.copy(deleted = true, pending = "DELETE"))
        return true
    }
    suspend fun saveDish(dish: DishEntity) {
        val current = dao.dish(dish.uid)
        dao.saveDish(dish.copy(remoteId = current?.remoteId,
            pending = if (current?.remoteId == null) "CREATE" else "UPDATE", deleted = false))
    }
    suspend fun deleteDish(uid: String) {
        val old = dao.dish(uid) ?: return
        if (old.remoteId == null) dao.removeDish(uid)
        else dao.saveDish(old.copy(pending = "DELETE", deleted = true))
    }
    suspend fun nameExists(name: String, categoryId: String, skipId: String? = null): Boolean =
        dao.allDishesRaw().any { !it.deleted && it.uid != skipId && it.categoryId == categoryId && it.name.trim().equals(name.trim(), true) }

    /** Push first, then pull. Dirty local rows win conflicts until uploaded. */
    suspend fun sync(): Boolean = lock.withLock {
        if (!syncConfigured()) return@withLock true // local-only until MockAPI configured
        try {
            val remoteCatsInitially = api.categories().associateBy { it.uid }
            for (item in dao.allCategoriesRaw().filter { it.pending != "" && !it.deleted }) {
                val existing = remoteCatsInitially[item.uid]
                val rid = item.remoteId ?: existing?.serverId
                val result = if (rid == null) api.addCategory(item.asRemote()) else api.editCategory(rid, item.asRemote())
                val latest = dao.category(item.uid)
                if (latest != null) {
                    val unchanged = latest.name == item.name && latest.description == item.description && !latest.deleted
                    dao.saveCategory(latest.copy(remoteId = result.serverId ?: rid,
                        pending = if (unchanged) "" else if (latest.deleted) "DELETE" else "UPDATE"))
                }
            }
            val remoteDishesInitially = api.dishes().associateBy { it.uid }
            for (item in dao.allDishesRaw().filter { it.pending != "" }) {
                if (item.deleted) {
                    val rid = item.remoteId ?: remoteDishesInitially[item.uid]?.serverId
                    if (rid != null) api.deleteDish(rid)
                    dao.removeDish(item.uid)
                } else {
                    val rid = item.remoteId ?: remoteDishesInitially[item.uid]?.serverId
                    val result = if (rid == null) api.addDish(item.asRemote()) else api.editDish(rid, item.asRemote())
                    val latest = dao.dish(item.uid)
                    if (latest != null) {
                        val unchanged = latest.asRemote() == item.asRemote() && !latest.deleted
                        dao.saveDish(latest.copy(remoteId = result.serverId ?: rid,
                            pending = if (unchanged) "" else if (latest.deleted) "DELETE" else "UPDATE"))
                    }
                }
            }
            for (item in dao.allCategoriesRaw().filter { it.deleted && it.pending != "" }) {
                val rid = item.remoteId ?: remoteCatsInitially[item.uid]?.serverId
                if (rid != null) api.deleteCategory(rid)
                dao.removeCategory(item.uid)
            }
            // Pull: add/update only non-dirty items, never overwrite an offline change.
            for (remote in api.categories()) {
                if (remote.uid.isBlank()) continue
                val local = dao.category(remote.uid)
                if (local == null || local.pending == "") {
                    dao.saveCategory(CategoryEntity(remote.uid, remote.name, remote.description, remote.serverId, ""))
                }
            }
            for (remote in api.dishes()) {
                if (remote.uid.isBlank()) continue
                val local = dao.dish(remote.uid)
                if (local == null || local.pending == "") {
                    dao.saveDish(DishEntity(remote.uid, remote.name, remote.price, remote.preparationTime,
                        remote.isAvailable, remote.description, remote.categoryId,
                        imagesToJson(remote.dishImage), remote.serverId, ""))
                }
            }
            pendingCount() == 0
        } catch (_: Exception) { false } // WorkManager retries on network/server failure
    }
}
