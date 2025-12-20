package com.example.game240;

// 沉浸式所需新增导入

import android.os.Build;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private TextView tv24PointRecords;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ========== 1. 刘海屏适配（和MainActivity一致） ==========
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.history); // 对应你的history.xml布局

        // ========== 2. 隐藏ActionBar（如果有） ==========
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // ========== 3. Android 11+ 粘性沉浸式（隐藏状态栏/导航栏） ==========
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false); // 内容延伸到系统栏区域
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                // 隐藏状态栏 + 导航栏
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                // 滑动边缘临时显示，无操作自动隐藏（粘性沉浸式）
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        // ========== 4. 去掉系统栏的padding（让内容占满屏幕） ==========
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            v.setPadding(0, 0, 0, 0); // 所有padding设为0
            return insets;
        });

        // ========== 原有逻辑 ==========
        ivBack = findViewById(R.id.fanhui);
        tv24PointRecords = findViewById(R.id.tv_24point_records);

        ivBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        // 读取数据库并显示记录
        loadHistoryRecords();
    }

    // 从数据库读取记录并显示到TextView
    private void loadHistoryRecords() {
        DBHelper dbHelper = new DBHelper(this);
        List<DBHelper.Record> records = dbHelper.queryAllTipRecords();

        // 拼接记录文本（初始为空）
        StringBuilder sb = new StringBuilder();
        if (records.isEmpty()) {
            sb.append("暂无提示记录"); // 无数据时显示
        } else {
            // 遍历记录，按「序号. 牌面 → 答案」格式拼接
            for (int i = 0; i < records.size(); i++) {
                DBHelper.Record record = records.get(i);
                sb.append((i + 1)).append(". ")
                        .append(record.cards)
                        .append(" → ")
                        .append(record.answer)
                        .append("\n");
            }
        }

        // 设置到TextView（ScrollView会自动滚动）
        tv24PointRecords.setText(sb.toString());
    }

    // ========== 可选：返回时恢复系统栏（优化体验） ==========
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 若需要在退出页面时恢复系统栏，可添加以下代码（可选）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        }
    }
}