package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edtUserName = findViewById<EditText>(R.id.edtUserName)
        val edtStudentId = findViewById<EditText>(R.id.edtStudentId)
        val btnClickMe = findViewById<Button>(R.id.btnClickMe)

        btnClickMe.setOnClickListener {
            val userName = edtUserName.text.toString().trim()
            val studentId = edtStudentId.text.toString().trim()

            // Kiểm tra họ tên không được để trống.
            if (userName.isEmpty()) {
                edtUserName.error = "Vui lòng nhập họ tên"
                edtUserName.requestFocus()
                return@setOnClickListener
            }

            // Kiểm tra MSSV không được để trống.
            if (studentId.isEmpty()) {
                edtStudentId.error = "Vui lòng nhập MSSV"
                edtStudentId.requestFocus()
                return@setOnClickListener
            }

            // Định dạng: B + 2 chữ cái + 22-26 + 4 chữ số.
            val studentIdPattern = Regex(
                "^B[A-Za-z]{2}(22|23|24|25|26)[0-9]{4}$"
            )

            if (!studentIdPattern.matches(studentId)) {
                edtStudentId.error =
                    "MSSV phải có dạng BXX22-26XXXX"
                edtStudentId.requestFocus()
                return@setOnClickListener
            }

            // Dữ liệu hợp lệ -> chuyển sang Screen 2.
            val intent = Intent(this, Screen2Activity::class.java)

            intent.putExtra("USER_NAME", userName)
            intent.putExtra("STUDENT_ID", studentId)

            startActivity(intent)
        }
    }
}