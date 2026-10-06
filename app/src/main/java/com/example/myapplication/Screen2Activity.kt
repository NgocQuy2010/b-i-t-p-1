package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Screen2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen2)

        // Lấy dữ liệu được truyền từ Screen 1.
        val userName = intent.getStringExtra("USER_NAME")
        val studentId = intent.getStringExtra("STUDENT_ID")

        // Lấy các TextView trên Screen 2.
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvStudentId = findViewById<TextView>(R.id.tvStudentId)

        // Hiển thị thông tin sinh viên.
        tvName.text = "Name: $userName"
        tvStudentId.text = "Student ID: $studentId"

        // Xử lý nút Back.
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }
    }
}