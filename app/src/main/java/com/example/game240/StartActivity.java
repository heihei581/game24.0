package com.example.game240;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定启动页布局 start.xml
        setContentView(R.layout.start);

        // 找到“练习模式”按钮（对应start.xml里的btn_practice_mode）
        Button btnPractice = findViewById(R.id.btn_practice_mode);

        // 按钮点击跳转到原游戏界面（MainActivity）
        btnPractice.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 可选：关闭启动页，返回时直接退出APP
        });
    }
}