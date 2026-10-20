package com.example.bit240200foodmanager.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes

val GREEN = Color.rgb(23, 107, 91)
val DARK = Color.rgb(25, 51, 47)
fun Context.dp(value: Int) = (value * resources.displayMetrics.density).toInt()
fun Context.label(text: CharSequence, size: Float = 15f, bold: Boolean = false, color: Int = DARK): TextView =
    TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        setPadding(dp(2), dp(5), dp(2), dp(5))
        if (bold) setTypeface(null, Typeface.BOLD)
    }
fun Context.button(@StringRes textId: Int, action: () -> Unit): Button = Button(this).apply {
    setText(textId); setOnClickListener { action() }; isAllCaps = false
}
fun Context.input(hintText: String, value: String = "", numerical: Boolean = false, lines: Int = 1): EditText =
    EditText(this).apply {
        hint = hintText; setText(value); textSize = 15f
        if (numerical) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        else if (lines > 1) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        else inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (lines > 1) { minLines = lines; gravity = Gravity.TOP or Gravity.START }
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }
fun Context.stack(horizontal: Boolean = false): LinearLayout = LinearLayout(this).apply {
    orientation = if (horizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
}
fun Context.scroller(child: View): ScrollView = ScrollView(this).apply { isFillViewport = true; addView(child) }
fun Activity.screen(title: String): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setBackgroundColor(Color.rgb(247, 250, 248))
    setPadding(dp(14), dp(12), dp(14), dp(10))
    addView(label(title, 23f, true, GREEN))
    setContentView(this)
}
fun Context.border(): GradientDrawable = GradientDrawable().apply {
    setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.rgb(222, 233, 228))
}
fun LinearLayout.addGap(heightDp: Int) { addView(View(context), LinearLayout.LayoutParams(1, context.dp(heightDp))) }
fun Context.formRow(@StringRes labelId: Int, entry: View): LinearLayout = stack().apply {
    addView(label(getString(labelId), 14f, true))
    addView(entry, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    addGap(8)
}
fun toast(context: Context, message: String) = android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
