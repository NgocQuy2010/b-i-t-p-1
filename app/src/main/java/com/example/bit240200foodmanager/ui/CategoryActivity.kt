package com.example.bit240200foodmanager.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bit240200foodmanager.FoodSyncScheduler
import com.example.bit240200foodmanager.R
import com.example.bit240200foodmanager.data.CategoryEntity
import com.example.bit240200foodmanager.data.FoodRepository
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {
    private lateinit var main: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        main = screen(getString(R.string.categories))
        val head = stack(true)
        head.addView(button(R.string.add_category) { edit(null) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(button(R.string.go_back) { finish() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        main.addView(head)
        val panel = stack()
        main.addView(scroller(panel), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        content = panel
        reload()
    }
    private lateinit var content: LinearLayout
    private fun reload() {
        lifecycleScope.launch {
            content.removeAllViews()
            val all = FoodRepository.categories()
            if (all.isEmpty()) content.addView(label(getString(R.string.no_categories)))
            all.forEach { c ->
                val tile = stack().apply { background = border(); setPadding(dp(12),dp(10),dp(12),dp(10)) }
                tile.addView(label(c.name, 18f, true, GREEN))
                tile.addView(label(c.description))
                val actions = stack(true)
                actions.addView(button(R.string.edit) { edit(c) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                actions.addView(button(R.string.delete_dish) { delete(c) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                tile.addView(actions); content.addView(tile); content.addGap(8)
            }
        }
    }
    private fun edit(model: CategoryEntity?) {
        val form = stack().apply { setPadding(dp(18), dp(10), dp(18), dp(10)) }
        val name = input(getString(R.string.name_hint), model?.name.orEmpty())
        val description = input(getString(R.string.description_hint), model?.description.orEmpty(), lines = 2)
        form.addView(formRow(R.string.name, name)); form.addView(formRow(R.string.description, description))
        val dialog = AlertDialog.Builder(this).setTitle(if (model == null) R.string.add_category else R.string.edit_category)
            .setView(form).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (name.text.toString().isBlank()) { name.error = getString(R.string.name_required); return@setOnClickListener }
                lifecycleScope.launch {
                    FoodRepository.saveCategory(model?.uid, name.text.toString(), description.text.toString())
                    FoodSyncScheduler.now(this@CategoryActivity)
                    dialog.dismiss(); reload()
                }
            }
        }
        dialog.show()
    }
    private fun delete(model: CategoryEntity) {
        AlertDialog.Builder(this).setTitle(R.string.confirm_delete).setMessage(R.string.delete_category_question)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_dish) { _, _ ->
                lifecycleScope.launch {
                    if (FoodRepository.deleteCategory(model.uid)) {
                        FoodSyncScheduler.now(this@CategoryActivity)
                        toast(this@CategoryActivity, getString(R.string.deleted))
                    } else toast(this@CategoryActivity, getString(R.string.category_in_use))
                    reload()
                }
            }.show()
    }
}
