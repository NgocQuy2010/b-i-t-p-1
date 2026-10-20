package com.example.bit240200foodmanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DishValidatorTest {
    private val category = setOf("BIT240200_cat1")
    private fun check(name: String="Pho", price: Double?=42000.0, time: Int?=15,
                      categoryId: String?="BIT240200_cat1", duplicate: Boolean=false) =
        DishValidator.validate(name, price, time, categoryId, category, duplicate)

    @Test fun requiresName() { assertEquals(DishValidator.Error.NAME, check(name = "  ")) }
    @Test fun positivePrice() { assertEquals(DishValidator.Error.PRICE, check(price = -1.0)) }
    @Test fun rejectsZeroTime() { assertEquals(DishValidator.Error.TIME, check(time = 0)) }
    @Test fun requiresExistingCategory() { assertEquals(DishValidator.Error.CATEGORY, check(categoryId = "missing")) }
    @Test fun detectsDuplicateInSameCategory() { assertEquals(DishValidator.Error.DUPLICATE_NAME, check(duplicate = true)) }
    @Test fun validDish() { assertNull(check()) }
}
