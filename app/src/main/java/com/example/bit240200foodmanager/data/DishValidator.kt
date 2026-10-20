package com.example.bit240200foodmanager.data

/** Business-rule validation independent of Android UI. */
object DishValidator {
    enum class Error { NAME, PRICE, TIME, CATEGORY, DUPLICATE_NAME }
    fun validate(name: String, price: Double?, minutes: Int?, categoryId: String?,
                 validCategoryIds: Set<String>, duplicateInCategory: Boolean): Error? = when {
        name.isBlank() -> Error.NAME
        price == null || !price.isFinite() || price <= 0 -> Error.PRICE
        minutes == null || minutes <= 0 -> Error.TIME
        categoryId == null || categoryId !in validCategoryIds -> Error.CATEGORY
        duplicateInCategory -> Error.DUPLICATE_NAME
        else -> null
    }
}
