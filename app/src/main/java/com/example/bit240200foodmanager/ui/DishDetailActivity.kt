package com.example.bit240200foodmanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.bit240200foodmanager.FoodSyncScheduler
import com.example.bit240200foodmanager.R
import com.example.bit240200foodmanager.data.FoodRepository
import kotlinx.coroutines.launch

class DishDetailActivity : AppCompatActivity() {
    private lateinit var main: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        main = screen(getString(R.string.dish_detail))
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val uid = intent.getStringExtra("uid").orEmpty()
            val dish = FoodRepository.dish(uid)
            main.removeViews(1, main.childCount - 1)
            if (dish == null) {
                main.addView(label(getString(R.string.not_found)))
                main.addView(button(R.string.go_back) { finish() })
                return@launch
            }
            val cat = FoodRepository.category(dish.categoryId)
            val panel = stack()
            val images = dish.images()
            panel.addView(label(dish.name, 23f, true, GREEN))
            if (images.isEmpty()) panel.addView(label(getString(R.string.no_images)))
            else {
                panel.addView(label(getString(R.string.photos), 16f, true))
                val horizontal = stack(true)
                images.forEach { photo ->
                    val box = stack()
                    val image = ImageView(this@DishDetailActivity).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    box.addView(image, LinearLayout.LayoutParams(dp(255), dp(165)))
                    val mark = if (photo.isThumbnail) " ★ ${getString(R.string.thumbnail)}" else ""
                    box.addView(label(mark, 12f, true, GREEN))
                    horizontal.addView(box)
                    Glide.with(this@DishDetailActivity).load(photo.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image).into(image)
                }
                panel.addView(HorizontalScrollView(this@DishDetailActivity).apply { addView(horizontal) })
            }
            val speed = if (dish.preparationTime <= 15) getString(R.string.quick) else getString(R.string.regular)
            val state = if (dish.isAvailable) getString(R.string.available) else getString(R.string.unavailable)
            panel.addView(label("${getString(R.string.category)}: ${cat?.name ?: getString(R.string.unknown_category)}"))
            panel.addView(label("${getString(R.string.price)}: ${dish.price.toLong()} ₫"))
            panel.addView(label("${getString(R.string.preparation_time)}: ${dish.preparationTime} min — $speed"))
            panel.addView(label("${getString(R.string.status)}: $state"))
            panel.addView(label("${getString(R.string.description)}: ${dish.description}"))
            panel.addGap(10)
            panel.addView(button(R.string.edit_dish) {
                startActivity(Intent(this@DishDetailActivity, DishFormActivity::class.java).putExtra("uid", uid))
            })
            panel.addView(button(R.string.delete_dish) {
                AlertDialog.Builder(this@DishDetailActivity).setTitle(R.string.confirm_delete)
                    .setMessage(R.string.delete_dish_question)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete_dish) { _, _ ->
                        lifecycleScope.launch {
                            FoodRepository.deleteDish(uid)
                            FoodSyncScheduler.now(this@DishDetailActivity)
                            toast(this@DishDetailActivity, getString(R.string.deleted))
                            finish()
                        }
                    }.show()
            })
            panel.addView(button(R.string.go_back) { finish() })
            main.addView(scroller(panel), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }
}
