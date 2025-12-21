package com.example.game240;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView tvLoading;
    private int progress = 0; // 当前加载进度
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定加载界面布局（activity_splash.xml）
        setContentView(R.layout.activity_splash);

        // ========== 核心：适配Android 12+返回键/返回手势，禁止返回 ==========
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 空实现：禁止加载过程中返回（包括按键返回/侧滑返回）
            }
        };
        // 将回调添加到系统返回调度器
        getOnBackPressedDispatcher().addCallback(this, callback);

        // ========== 绑定控件 + 模拟加载逻辑 ==========
        // 初始化控件
        progressBar = findViewById(R.id.progress_bar);
        tvLoading = findViewById(R.id.tv_loading);
        handler = new Handler(Looper.getMainLooper());

        // 模拟加载进度更新（实际项目替换为真实初始化逻辑）
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                progress += 1;
                // 更新进度条和文本
                progressBar.setProgress(progress);
                tvLoading.setText("加载中... " + progress + "%");

                // 进度未到100则继续更新（每50ms更新一次，可调整速度）
                if (progress < 100) {
                    handler.postDelayed(this, 50);
                } else {
                    // 加载完成，跳转到你的目标界面 StartActivity
                    Intent intent = new Intent(SplashActivity.this, StartActivity.class);
                    startActivity(intent);
                    // 关闭加载界面，避免返回键回到此页面
                    finish();
                }
            }
        };
        // 启动进度更新
        handler.post(runnable);
    }

    // ========== 防止内存泄漏：销毁时移除未执行的handler任务 ==========
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}