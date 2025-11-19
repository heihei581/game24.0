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

    //存储扑克牌数值和状态
    private int[] cardValues = {6, 6, 6, 6};
    private boolean[] cardUsed = new boolean[4]; // 记录每张牌是否被使用
    private ImageView[] cardViews; // 存储四个ImageView的引用
    private EditText etExpression; //输入框引用

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

        // 随机换牌功能
        // 获取四个扑克牌ImageView
        final ImageView card1 = findViewById(R.id.card1);
        final ImageView card2 = findViewById(R.id.card2);
        final ImageView card3 = findViewById(R.id.card3);
        final ImageView card4 = findViewById(R.id.card4);

        // 初始化数组
        cardViews = new ImageView[]{card1, card2, card3, card4};
        etExpression = findViewById(R.id.et_expression);

        // 【新增】初始化扑克牌点击事件
        setupCardClickListeners();

        // 【新增】获取清空按钮
        Button btnClear = findViewById(R.id.btn_clear);
        // 【新增】清空按钮点击事件
        btnClear.setOnClickListener(v -> {
            etExpression.setText(""); // 清空输入框
            enableAllCards(); // 启用所有扑克牌
        });

        // 获取刷新按钮
        Button btnRefresh = findViewById(R.id.btn_Refresh);
        // 刷新按钮点击事件：随机抽取4张牌（允许重复）
        btnRefresh.setOnClickListener(v -> {
            Random random = new Random();

            for (int i = 0; i < cardViews.length; i++) {
                ImageView card = cardViews[i];
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
                            // 阶段2：换牌并生成新数值
                            int randomNum = random.nextInt(10) + 1;
                            cardValues[index] = randomNum; // 保存新数值
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

            // 【新增】刷新后启用所有牌
            enableAllCards();
            etExpression.setText(""); // 清空输入框
        });


        // 【新增】获取符号按钮并设置点击事件（最小化修改方案）
        findViewById(R.id.btn_add).setOnClickListener(v -> appendToExpression("+"));
        findViewById(R.id.btn_sub).setOnClickListener(v -> appendToExpression("-"));
        findViewById(R.id.btn_mul).setOnClickListener(v -> appendToExpression("*"));
        findViewById(R.id.btn_div).setOnClickListener(v -> appendToExpression("/"));
        findViewById(R.id.btn_right).setOnClickListener(v -> appendToExpression("("));
        findViewById(R.id.btn_left).setOnClickListener(v -> appendToExpression(")"));

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

    // 【新增】设置扑克牌点击事件 循环给扑克牌加
    private void setupCardClickListeners() {
        for (int i = 0; i < cardViews.length; i++) {
            final int index = i;
            cardViews[i].setOnClickListener(v -> {
                if (!cardUsed[index]) {
                    // 将数字添加到输入框
                    String currentText = etExpression.getText().toString();
                    etExpression.setText(currentText + cardValues[index]);

                    // 禁用该扑克牌
                    disableCard(index);
                }
            });
        }
    }

    // 禁用扑克牌
    private void disableCard(int index) {
        cardUsed[index] = true;
        cardViews[index].setAlpha(0.5f); // 半透明
        cardViews[index].setClickable(false); // 不可点击
    }

    // 【新增】启用所有扑克牌
    private void enableAllCards() {
        for (int i = 0; i < 4; i++) {
            cardUsed[i] = false;
            cardViews[i].setAlpha(1.0f); // 不透明
            cardViews[i].setClickable(true); // 可点击
        }
    }

    // 【新增】向输入框追加符号（最小化代码修改）
    private void appendToExpression(String symbol) {
        String currentText = etExpression.getText().toString();
        etExpression.setText(currentText + symbol);
    }
}