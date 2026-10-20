package com.example.bit240200foodmanager.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.bit240200foodmanager.FoodSyncScheduler
import com.example.bit240200foodmanager.R
import com.example.bit240200foodmanager.data.CategoryEntity
import com.example.bit240200foodmanager.data.DishEntity
import com.example.bit240200foodmanager.data.DishImage
import com.example.bit240200foodmanager.data.DishValidator
import com.example.bit240200foodmanager.data.FoodRepository
import com.example.bit240200foodmanager.data.imagesToJson
import kotlinx.coroutines.launch
import java.net.URI
import java.util.UUID

class DishFormActivity : AppCompatActivity() {
    private lateinit var name: EditText
    private lateinit var price: EditText
    private lateinit var minutes: EditText
    private lateinit var description: EditText
    private lateinit var available: Switch
    private lateinit var category: Spinner
    private lateinit var urlInput: EditText
    private lateinit var imagePanel: LinearLayout
    private val categories = mutableListOf<CategoryEntity>()
    private val images = mutableListOf<String>()
    private var thumb = -1
    private var editUid: String? = null
    private lateinit var main: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editUid = intent.getStringExtra("uid")
        main = screen(getString(if (editUid == null) R.string.add_dish else R.string.edit_dish))
        val form = stack()
        main.addView(scroller(form), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        name = input(getString(R.string.name_hint))
        price = input(getString(R.string.price_hint), numerical = true)
        minutes = input(getString(R.string.minutes_hint), numerical = true)
        description = input(getString(R.string.description_hint), lines = 3)
        category = Spinner(this)
        available = Switch(this).apply { setText(R.string.available); isChecked = true }
        form.addView(formRow(R.string.name, name)); form.addView(formRow(R.string.description, description))
        form.addView(formRow(R.string.price, price)); form.addView(formRow(R.string.preparation_time, minutes))
        form.addView(formRow(R.string.category, category)); form.addView(available)
        form.addGap(12)
        form.addView(label(getString(R.string.dish_images), 17f, true, GREEN))
        form.addView(label(getString(R.string.images_help), 12f))
        urlInput = input("https://...", lines = 1)
        form.addView(urlInput)
        form.addView(button(R.string.add_image) { addImage() })
        imagePanel = stack()
        form.addView(imagePanel)
        form.addGap(12)
        form.addView(button(R.string.save) { save() })
        form.addView(button(R.string.cancel) { finish() })
        lifecycleScope.launch {
            categories.addAll(FoodRepository.categories())
            if (categories.isEmpty()) {
                toast(this@DishFormActivity, getString(R.string.create_category_first))
                finish(); return@launch
            }
            category.adapter = ArrayAdapter(this@DishFormActivity, android.R.layout.simple_spinner_dropdown_item, categories.map { it.name })
            val previous = editUid?.let { FoodRepository.dish(it) }
            if (editUid != null && previous == null) {
                toast(this@DishFormActivity, getString(R.string.not_found)); finish(); return@launch
            }
            previous?.let { d ->
                name.setText(d.name); description.setText(d.description)
                price.setText(d.price.toString()); minutes.setText(d.preparationTime.toString())
                available.isChecked = d.isAvailable
                val idx = categories.indexOfFirst { it.uid == d.categoryId }
                if (idx >= 0) category.setSelection(idx)
                images.clear(); images.addAll(d.images().map { it.imageUrl })
                thumb = d.images().indexOfFirst { it.isThumbnail }
                if (thumb == -1 && images.isNotEmpty()) thumb = 0
                renderImages()
            }
        }
    }
    private fun validUrl(value: String): Boolean = try {
        val uri = URI(value.trim()); uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }
    private fun addImage() {
        val value = urlInput.text.toString().trim()
        if (!validUrl(value)) { urlInput.error = getString(R.string.invalid_image_url); return }
        if (images.any { it == value }) { urlInput.error = getString(R.string.duplicate_url); return }
        images.add(value)
        if (thumb == -1) thumb = 0
        urlInput.setText(""); renderImages()
    }
    private fun renderImages() {
        imagePanel.removeAllViews()
        images.forEachIndexed { index, url ->
            val row = stack(true).apply { background = border(); setPadding(dp(6), dp(5), dp(6), dp(5)) }
            val preview = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            row.addView(preview, LinearLayout.LayoutParams(dp(95), dp(78)))
            Glide.with(this).load(url).placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image).into(preview)
            val info = stack()
            val choice = RadioButton(this).apply {
                text = getString(R.string.thumbnail); isChecked = index == thumb
                setOnClickListener { thumb = index; renderImages() }
            }
            info.addView(choice)
            info.addView(label(url, 11f))
            info.addView(button(R.string.remove_image) {
                images.removeAt(index)
                thumb = when { images.isEmpty() -> -1; index == thumb -> 0; index < thumb -> thumb - 1; else -> thumb }
                renderImages()
            })
            row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            imagePanel.addView(row)
            imagePanel.addGap(7)
        }
        if (images.isEmpty()) imagePanel.addView(label(getString(R.string.no_images)))
    }
    private fun save() {
        lifecycleScope.launch {
            val textName = name.text.toString().trim()
            val p = price.text.toString().toDoubleOrNull()
            val t = minutes.text.toString().toIntOrNull()
            val chosen = categories.getOrNull(category.selectedItemPosition)
            val duplicate = chosen != null && FoodRepository.nameExists(textName, chosen.uid, editUid)
            when (DishValidator.validate(textName, p, t, chosen?.uid,
                categories.map { it.uid }.toSet(), duplicate)) {
                DishValidator.Error.NAME -> { name.error = getString(R.string.name_required); name.requestFocus(); return@launch }
                DishValidator.Error.PRICE -> { price.error = getString(R.string.price_positive); price.requestFocus(); return@launch }
                DishValidator.Error.TIME -> { minutes.error = getString(R.string.time_positive); minutes.requestFocus(); return@launch }
                DishValidator.Error.CATEGORY -> { toast(this@DishFormActivity, getString(R.string.invalid_category)); return@launch }
                DishValidator.Error.DUPLICATE_NAME -> { name.error = getString(R.string.duplicate_name); name.requestFocus(); return@launch }
                null -> Unit
            }
            val list = images.mapIndexed { i, s -> DishImage(s, i == thumb) }
            val dish = DishEntity(editUid ?: UUID.randomUUID().toString(), textName, p!!, t!!,
                available.isChecked, description.text.toString().trim(), chosen!!.uid, imagesToJson(list))
            FoodRepository.saveDish(dish)
            FoodSyncScheduler.now(this@DishFormActivity)
            toast(this@DishFormActivity, getString(R.string.saved_offline))
            finish()
        }
    }
}
