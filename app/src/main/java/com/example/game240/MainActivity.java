package com.example.game240;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //存储扑克牌数值和状态
    private ImageView[] cardViews; // 扑克牌的图片
    private int[] cardValues = {6,6,6,6};//扑克牌的值
    private boolean[] cardUsed = new boolean[4]; // 记录每张牌是否被使用
    private EditText etExpression; //输入框引用
    private JexlEngine jexlEngine;//第三方库计算引擎

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

        // 初始化函数，启动就干的
        findViews();//绑定扑克牌并初始化cardViews数组
        setupJexlEngine();//初始化计算引擎
        setupCardClickListeners();//扑克牌监听
        setupButtonListeners();//按钮监听
        disableSoftKeyboard();//静止使用软键盘
    }

    private void findViews() {
        etExpression = findViewById(R.id.et_expression);
        // 获取四个扑克牌ImageView
        final ImageView card1 = findViewById(R.id.card1);
        final ImageView card2 = findViewById(R.id.card2);
        final ImageView card3 = findViewById(R.id.card3);
        final ImageView card4 = findViewById(R.id.card4);
        // 初始化数组
        cardViews = new ImageView[]{card1, card2, card3, card4};
    }

    private void setupJexlEngine() {
        //初始化JEXL引擎
        jexlEngine = new JexlBuilder().create();
    }

    private void setupButtonListeners() {
        //获取清空按钮并设置点击事件
        Button btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(v -> {
            clear();
        });

        // 获取刷新按钮并设置点击事件
        Button btnRefresh = findViewById(R.id.btn_Refresh);
        btnRefresh.setOnClickListener(v -> {
            refreshCards();
            clear();
        });

        //获取符号按钮并设置点击事件
        findViewById(R.id.btn_add).setOnClickListener(v -> appendToExpression("+"));
        findViewById(R.id.btn_sub).setOnClickListener(v -> appendToExpression("-"));
        findViewById(R.id.btn_mul).setOnClickListener(v -> appendToExpression("*"));
        findViewById(R.id.btn_div).setOnClickListener(v -> appendToExpression("/"));
        findViewById(R.id.btn_right).setOnClickListener(v -> appendToExpression("("));
        findViewById(R.id.btn_left).setOnClickListener(v -> appendToExpression(")"));

        //获取提交按钮并设置点击事件
        Button btnSubmit = findViewById(R.id.btn_submit);
        btnSubmit.setOnClickListener(v -> submitExpression());
    }

    //禁用软键盘
    private void disableSoftKeyboard() {
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

    private void submitExpression() {
        String expression = etExpression.getText().toString().trim();
        if (expression.isEmpty()) {
            Toast.makeText(this, "请输入计算式", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否所有牌都已使用
        if (!isAllCardsUsed()) {
            Toast.makeText(this, "必须使用所有4张牌进行计算", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 1. 创建一个表达式对象
            JexlExpression jexlExpression = jexlEngine.createExpression(expression);
            // 2. 执行表达式并获取结果
            Object resultObject = jexlExpression.evaluate(null);
            // 确保结果是一个数字
            if (resultObject instanceof Number) {
                double result = ((Number) resultObject).doubleValue();

                // 判断结果是否为24
                if (Math.abs(result - 24) < 1e-6) {
                    Toast.makeText(this, "计算正确！", Toast.LENGTH_SHORT).show();
                    // 延迟1秒后刷新牌面
                    new Handler().postDelayed(() -> {
                        enableAllCards();
                        refreshCards();
                        etExpression.setText("");
                    }, 1000);
                } else {
                    Toast.makeText(this, "计算错误，请重试", Toast.LENGTH_SHORT).show();
                    enableAllCards();
                    clear();
                }
            } else {
                Toast.makeText(this, "表达式结果不是一个有效的数字", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "表达式格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //设置扑克牌点击事件 循环给扑克牌加
    private void setupCardClickListeners() {
        for (int i = 0; i < cardViews.length; i++) {
            final int index = i;
            cardViews[i].setOnClickListener(v -> {
                if (!cardUsed[index]) {
                    // 将数字添加到输入框
                    appendToExpression(String.valueOf(cardValues[index]));

                    // 禁用该扑克牌
                    disableCard(index);
                }
            });
        }
    }

    private void refreshCards() {
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
    }

    // 判断四张牌是否都使用了
    private boolean isAllCardsUsed() {
        for (boolean used : cardUsed) {
            if (!used) { // 只要有一张牌未使用，就返回false
                return false;
            }
        }
        return true; // 所有牌都已使用，返回true
    }

    // 禁用扑克牌
    private void disableCard(int index) {
        cardUsed[index] = true;
        cardViews[index].setAlpha(0.5f); // 半透明
        cardViews[index].setClickable(false); // 不可点击
    }

    //启用所有扑克牌
    private void enableAllCards() {
        for (int i = 0; i < 4; i++) {
            cardUsed[i] = false;
            cardViews[i].setAlpha(1.0f); // 不透明
            cardViews[i].setClickable(true); // 可点击
        }
    }

    private void clear()
    {
        etExpression.setText(""); // 清空输入框
        enableAllCards(); // 启用所有扑克牌
    }

    //向输入框追加符号
    private void appendToExpression(String symbol) {
        String currentText = etExpression.getText().toString();
        etExpression.setText(currentText + symbol);
    }
}