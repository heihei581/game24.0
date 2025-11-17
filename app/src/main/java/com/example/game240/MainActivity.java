package com.example.game240;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 【新增】随机换牌功能
        // 获取四个扑克牌ImageView
        final ImageView card1 = findViewById(R.id.card1);
        final ImageView card2 = findViewById(R.id.card2);
        final ImageView card3 = findViewById(R.id.card3);
        final ImageView card4 = findViewById(R.id.card4);

        // 获取刷新按钮
        Button btnRefresh = findViewById(R.id.btn_Refresh);

        // 设置点击事件：随机抽取4张牌（允许重复）
        btnRefresh.setOnClickListener(v -> {
            Random random = new Random();
            ImageView[] cards = {card1, card2, card3, card4};

            for (int i = 0; i < cards.length; i++) {
                ImageView card = cards[i];
                final int index = i; // 需要final用于lambda

                // 阶段1：缩小+旋转+淡出（洗牌动作）
                card.animate()
                        .scaleX(0.6f)           // 缩小到60%
                        .scaleY(0.6f)
                        .rotation(360)          // 快速旋转360度
                        .alpha(0.3f)            // 透明度30%
                        .setDuration(350)
                        .setStartDelay(i * 70)  // 依次延迟，有飞出的层次感
                        .withEndAction(() -> {
                            // 阶段2：换牌
                            int randomNum = random.nextInt(10) + 1;
                            int drawableId = getResources().getIdentifier(
                                    "c" + randomNum, "drawable", getPackageName()
                            );
                            card.setImageResource(drawableId);

                            // 阶段3：弹性放大+淡入（恢复）
                            card.animate()
                                    .scaleX(1f)       // 弹回原始大小
                                    .scaleY(1f)
                                    .rotation(0)      // 停止旋转
                                    .alpha(1f)        // 恢复不透明
                                    .setDuration(300)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator()); // 弹性效果
                        });
            }
        });

        EditText etExpression = findViewById(R.id.et_expression);

        // 禁用软键盘
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            etExpression.setShowSoftInputOnFocus(false);
        } else {
            etExpression.setOnTouchListener((v, event) -> {
                // 消费掉触摸事件，阻止键盘弹出
                return true;  // 返回true表示事件已处理
            });
        }


    }
}