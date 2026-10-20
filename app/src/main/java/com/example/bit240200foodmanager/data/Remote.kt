package com.example.bit240200foodmanager.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// MockAPI automatically adds its reserved 'id'. Every user-defined remote field is prefixed.
data class CategoryRemote(
    @SerializedName("id") val serverId: String? = null,
    @SerializedName("BIT240200_Id") val uid: String = "",
    @SerializedName("BIT240200_Name") val name: String = "",
    @SerializedName("BIT240200_Description") val description: String = ""
)
data class DishRemote(
    @SerializedName("id") val serverId: String? = null,
    @SerializedName("BIT240200_Id") val uid: String = "",
    @SerializedName("BIT240200_Name") val name: String = "",
    @SerializedName("BIT240200_Price") val price: Double = 0.0,
    @SerializedName("BIT240200_PreparationTime") val preparationTime: Int = 0,
    @SerializedName("BIT240200_IsAvailable") val isAvailable: Boolean = true,
    @SerializedName("BIT240200_Description") val description: String = "",
    @SerializedName("BIT240200_DishCategoryId") val categoryId: String = "",
    @SerializedName("BIT240200_DishImage") val dishImage: List<DishImage> = emptyList()
)
fun CategoryEntity.asRemote() = CategoryRemote(uid = uid, name = name, description = description)
fun DishEntity.asRemote() = DishRemote(uid = uid, name = name, price = price,
    preparationTime = preparationTime, isAvailable = isAvailable, description = description,
    categoryId = categoryId, dishImage = images())

interface FoodApi {
    @GET("BIT240200_DishCategory") suspend fun categories(): List<CategoryRemote>
    @GET("BIT240200_Dish") suspend fun dishes(): List<DishRemote>
    @POST("BIT240200_DishCategory") suspend fun addCategory(@Body data: CategoryRemote): CategoryRemote
    @POST("BIT240200_Dish") suspend fun addDish(@Body data: DishRemote): DishRemote
    @PUT("BIT240200_DishCategory/{id}") suspend fun editCategory(@Path("id") id: String, @Body data: CategoryRemote): CategoryRemote
    @PUT("BIT240200_Dish/{id}") suspend fun editDish(@Path("id") id: String, @Body data: DishRemote): DishRemote
    @DELETE("BIT240200_DishCategory/{id}") suspend fun deleteCategory(@Path("id") id: String)
    @DELETE("BIT240200_Dish/{id}") suspend fun deleteDish(@Path("id") id: String)
}
