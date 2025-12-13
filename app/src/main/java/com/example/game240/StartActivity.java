package com.example.game240;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定启动页布局 start.xml
        setContentView(R.layout.start);

        // 1. 绑定控件
        Button btnPractice = findViewById(R.id.btn_practice_mode);
        Button btnTime = findViewById(R.id.btn_time_limit_mode);
        EditText etTimeSetting = findViewById(R.id.et_time_setting); // 时间输入框

        // 2. 练习模式点击事件（原有逻辑不变）
        btnPractice.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 可选：关闭启动页，返回时直接退出APP
        });

        // 3. 限时模式点击事件（新增时间参数传递逻辑）
        btnTime.setOnClickListener(v -> {
            // 3.1 获取输入框的时间（秒数）
            String timeInput = etTimeSetting.getText().toString().trim();
            long setTimeSeconds = 500; // 默认500秒（兼容原有逻辑）

            // 3.2 校验输入的数值
            if (!timeInput.isEmpty()) {
                try {
                    setTimeSeconds = Long.parseLong(timeInput);
                    // 禁止输入0或负数
                    if (setTimeSeconds <60) {
                        Toast.makeText(this, "请输入大于等于60的秒数", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 可选：限制最大时间（比如1800秒=30分钟，避免输入过大）
                    if (setTimeSeconds > 1800) {
                        Toast.makeText(this, "最大支持10分钟（600秒）", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    // 输入非数字时提示
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 3.3 传递时间参数到限时模式Activity
            Intent intent = new Intent(StartActivity.this, Time_model.class);
            intent.putExtra("CUSTOM_TIME_SECONDS", setTimeSeconds); // 传递秒数
            startActivity(intent);
            finish(); // 可选：关闭启动页
        });
    }
}