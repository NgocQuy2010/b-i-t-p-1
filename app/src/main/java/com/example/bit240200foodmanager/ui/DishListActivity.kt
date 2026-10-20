package com.example.bit240200foodmanager.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bit240200foodmanager.FoodSyncScheduler
import com.example.bit240200foodmanager.R
import com.example.bit240200foodmanager.data.CategoryEntity
import com.example.bit240200foodmanager.data.DishEntity
import com.example.bit240200foodmanager.data.FoodRepository
import kotlinx.coroutines.launch

class DishListActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("BIT240200_filters", MODE_PRIVATE) }
    private lateinit var adapter: DishAdapter
    private lateinit var search: EditText
    private lateinit var category: Spinner
    private lateinit var sort: Spinner
    private lateinit var activeOnly: Switch
    private lateinit var minimum: EditText
    private lateinit var maximum: EditText
    private lateinit var emptyView: TextView
    private lateinit var counter: TextView
    private lateinit var syncMessage: TextView
    private val categoryItems = mutableListOf<CategoryEntity>()
    private var allDishes = listOf<DishEntity>()
    private var maps = mapOf<String, String>()
    private var selectedCategory = ""
    private var restoringFilters = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val main = screen(getString(R.string.app_title))
        val upper = stack(true)
        upper.addView(button(R.string.categories) {
            startActivity(Intent(this, CategoryActivity::class.java))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        upper.addView(button(R.string.add_dish) {
            startActivity(Intent(this, DishFormActivity::class.java))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        main.addView(upper)
        val lang = button(R.string.change_language) {
            val now = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val next = if (now.startsWith("vi")) "en" else "vi"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
        }
        main.addView(lang)
        val filterBox = stack().apply { background = border(); setPadding(dp(10), dp(8), dp(10), dp(8)) }
        search = input(getString(R.string.search_hint), prefs.getString("q", "") ?: "")
        filterBox.addView(search)
        val group = stack(true)
        category = Spinner(this)
        sort = Spinner(this)
        group.addView(category, LinearLayout.LayoutParams(0, dp(48), 1f))
        group.addView(sort, LinearLayout.LayoutParams(0, dp(48), 1f))
        filterBox.addView(group)
        activeOnly = Switch(this).apply { text = getString(R.string.available_only); isChecked = prefs.getBoolean("active", false) }
        filterBox.addView(activeOnly)
        val priceGroup = stack(true)
        minimum = input(getString(R.string.min_price), prefs.getString("min", "") ?: "", true)
        maximum = input(getString(R.string.max_price), prefs.getString("max", "") ?: "", true)
        priceGroup.addView(minimum, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        priceGroup.addView(maximum, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        filterBox.addView(priceGroup)
        filterBox.addView(button(R.string.reset_filters) { resetFilters() })
        main.addView(filterBox)
        counter = label(getString(R.string.loading), 13f, true)
        main.addView(counter)
        syncMessage = label("", 12f, true, GREEN)
        main.addView(syncMessage)
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("BIT240200_sync_now").observe(this) {
            lifecycleScope.launch { updateSyncStatus() }
        }
        val list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@DishListActivity) }
        adapter = DishAdapter { dish -> startActivity(Intent(this, DishDetailActivity::class.java).putExtra("uid", dish.uid)) }
        list.adapter = adapter
        main.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        emptyView = label(getString(R.string.no_dishes))
        main.addView(emptyView)
        fun onchange() { if (!restoringFilters) { saveState(); filter() } }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onchange()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        listOf(search, minimum, maximum).forEach { it.addTextChangedListener(watcher) }
        activeOnly.setOnCheckedChangeListener { _, _ -> onchange() }
        val sorts = listOf(getString(R.string.sort_name),getString(R.string.sort_price_asc),
            getString(R.string.sort_price_desc),getString(R.string.sort_time))
        sort.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sorts)
        sort.setSelection(prefs.getInt("sort", 0))
        sort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = onchange()
        }
        category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) "" else categoryItems.getOrNull(position - 1)?.uid.orEmpty()
                onchange()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            FoodRepository.seedOnce(this@DishListActivity)
            categoryItems.clear(); categoryItems.addAll(FoodRepository.categories())
            allDishes = FoodRepository.dishes()
            maps = categoryItems.associate { it.uid to it.name }
            val desired = prefs.getString("category", "").orEmpty()
            restoringFilters = true
            val labels = listOf(getString(R.string.all_categories)) + categoryItems.map { it.name }
            category.adapter = ArrayAdapter(this@DishListActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            val idx = categoryItems.indexOfFirst { it.uid == desired }
            category.setSelection(if (idx == -1) 0 else idx + 1)
            selectedCategory = if (idx == -1) "" else desired
            restoringFilters = false
            filter()
            updateSyncStatus()
            FoodSyncScheduler.now(this@DishListActivity)
        }
    }
    private suspend fun updateSyncStatus() {
        syncMessage.text = if (!FoodRepository.syncConfigured()) getString(R.string.sync_local)
        else {
            val pending = FoodRepository.pendingCount()
            if (pending > 0) getString(R.string.sync_pending, pending) else getString(R.string.sync_ready)
        }
    }
    private fun resetFilters() {
        search.setText(""); minimum.setText(""); maximum.setText("")
        activeOnly.isChecked = false; category.setSelection(0); sort.setSelection(0)
        selectedCategory = ""; saveState(); filter()
    }
    private fun saveState() {
        prefs.edit().putString("q", search.text.toString()).putString("category", selectedCategory)
            .putBoolean("active", activeOnly.isChecked).putString("min", minimum.text.toString())
            .putString("max", maximum.text.toString()).putInt("sort", sort.selectedItemPosition.coerceAtLeast(0)).apply()
    }
    private fun filter() {
        if (!::adapter.isInitialized) return
        val lo = minimum.text.toString().toDoubleOrNull()
        val hi = maximum.text.toString().toDoubleOrNull()
        if ((minimum.text.isNotBlank() && lo == null) || (maximum.text.isNotBlank() && hi == null) ||
            (lo != null && lo < 0) || (hi != null && hi < 0) || (lo != null && hi != null && lo > hi)) {
            counter.text = getString(R.string.invalid_price_range)
            adapter.submit(emptyList(), maps); emptyView.visibility = View.VISIBLE
            return
        }
        val q = search.text.toString().trim()
        var result = allDishes.filter { d ->
            d.name.contains(q, ignoreCase = true) && (selectedCategory.isEmpty() || d.categoryId == selectedCategory) &&
                (!activeOnly.isChecked || d.isAvailable) && (lo == null || d.price >= lo) && (hi == null || d.price <= hi)
        }
        result = when (sort.selectedItemPosition) {
            1 -> result.sortedBy { it.price }; 2 -> result.sortedByDescending { it.price }
            3 -> result.sortedBy { it.preparationTime }; else -> result.sortedBy { it.name.lowercase() }
        }
        adapter.submit(result, maps)
        counter.text = getString(R.string.item_count, result.size)
        emptyView.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }
}

class DishAdapter(private val selected: (DishEntity) -> Unit) : RecyclerView.Adapter<DishAdapter.Holder>() {
    private var items = listOf<DishEntity>()
    private var cats = mapOf<String,String>()
    fun submit(data: List<DishEntity>, categories: Map<String,String>) {
        items = data; cats = categories; notifyDataSetChanged()
    }
    class Holder(val row: LinearLayout, val img: android.widget.ImageView, val text: TextView) : RecyclerView.ViewHolder(row)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val c = parent.context
        val row = c.stack(true).apply { background = c.border(); setPadding(c.dp(10), c.dp(10), c.dp(8), c.dp(10)) }
        val img = android.widget.ImageView(c).apply { scaleType = android.widget.ImageView.ScaleType.CENTER_CROP }
        row.addView(img, LinearLayout.LayoutParams(c.dp(100), c.dp(105)))
        val text = c.label("", 15f, true)
        text.setPadding(c.dp(12), c.dp(3), c.dp(4), c.dp(3))
        row.addView(text, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = c.dp(8)
        }
        return Holder(row, img, text)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val d = items[position]; val c = holder.row.context
        val state = c.getString(if (d.isAvailable) R.string.available else R.string.unavailable)
        val speed = c.getString(if (d.preparationTime <= 15) R.string.quick else R.string.regular)
        holder.text.text = "${d.name}\n${d.price.toLong()} ₫ • ${cats[d.categoryId] ?: c.getString(R.string.unknown_category)}\n${d.preparationTime} min • $speed\n$state"
        Glide.with(c).load(d.thumbnail()).placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image).into(holder.img)
        holder.row.setOnClickListener { selected(d) }
    }
}
