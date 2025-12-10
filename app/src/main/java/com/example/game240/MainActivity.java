package com.example.game240;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.example.game240.Calculate24;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //存储扑克牌数值和状态
    private ImageView[] cardViews; // 扑克牌的图片
    private int[] cardValues = {6,6,6,6};//扑克牌的值
    private boolean[] cardUsed = new boolean[4]; // 记录每张牌是否被使用
    private EditText etExpression; //输入框引用
    private JexlEngine jexlEngine;//第三方库计算引擎
    private boolean lastClickedIsCard = false; //记录最后一次是否点击的是扑克牌
    private String result="6+6+6+6";//初始牌解

    // Toast防抖
    private boolean isToastShowing = false; // 标记Toast是否在冷却期
    private Toast currentToast; // 当前显示的Toast对象
    private Handler toastHandler = new Handler(Looper.getMainLooper());
    private static final long TOAST_DEBOUNCE_TIME = 2000; // Toast防抖冷却时间（毫秒）

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

    // 统一的防抖Toast显示方法
    private void showDebouncedToast(String message, int duration) {
        // 取消之前未显示完的Toast
        if (currentToast != null) {
            currentToast.cancel();
        }

        // 如果不在冷却期，显示Toast
        if (!isToastShowing) {
            isToastShowing = true;
            currentToast = Toast.makeText(this, message, duration);
            currentToast.show();

            // 冷却期结束后重置状态
            toastHandler.postDelayed(() -> {
                isToastShowing = false;
                currentToast = null;
            }, TOAST_DEBOUNCE_TIME);
        }
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

        // 获取提示按钮并设置点击事件
        Button btnTips = findViewById(R.id.btn_Tips);
        btnTips.setOnClickListener(v -> {
            Tips();
        });

        //获取符号按钮并设置点击事件（点击符号按钮后重置扑克牌状态）
        findViewById(R.id.btn_add).setOnClickListener(v -> {
            appendToExpression("+");
            lastClickedIsCard = false;
        });
        findViewById(R.id.btn_sub).setOnClickListener(v -> {
            appendToExpression("-");
            lastClickedIsCard = false;
        });
        findViewById(R.id.btn_mul).setOnClickListener(v -> {
            appendToExpression("*");
            lastClickedIsCard = false;
        });
        findViewById(R.id.btn_div).setOnClickListener(v -> {
            appendToExpression("/");
            lastClickedIsCard = false;
        });
        findViewById(R.id.btn_right).setOnClickListener(v -> {
            appendToExpression("(");
            lastClickedIsCard = false;
        });
        findViewById(R.id.btn_left).setOnClickListener(v -> {
            appendToExpression(")");
            lastClickedIsCard = false;
        });

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
            showDebouncedToast("请输入计算式", Toast.LENGTH_SHORT);
            return;
        }

        // 检查是否所有牌都已使用
        if (!isAllCardsUsed()) {
            showDebouncedToast("必须使用所有4张牌进行计算", Toast.LENGTH_SHORT);
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
                    showDebouncedToast("计算正确！", Toast.LENGTH_SHORT);
                    // 延迟1秒后刷新牌面
                    new Handler().postDelayed(() -> {
                        enableAllCards();
                        enableOP();
                        refreshCards();
                        etExpression.setText("");
                        lastClickedIsCard = false; // 重置牌点击状态
                    }, 1000);
                }
                else {
                    showDebouncedToast("计算错误 结果为："+result, Toast.LENGTH_SHORT);
                    enableAllCards();
                    enableOP();
                    etExpression.setText("");
                    lastClickedIsCard = false;
                }
            }
            else {
                showDebouncedToast("表达式结果不是一个有效的数字", Toast.LENGTH_SHORT);
            }

        } catch (Exception e) {
            showDebouncedToast("算式不合法" , Toast.LENGTH_SHORT);
        }
    }

    //设置扑克牌点击事件 循环给扑克牌加
    private void setupCardClickListeners() {
        for (int i = 0; i < cardViews.length; i++) {
            final int index = i;
            cardViews[i].setOnClickListener(v -> {
                 //判断是否连续点击牌，且当前牌未使用
                if (!cardUsed[index] && !lastClickedIsCard) {
                    // 将数字添加到输入框
                    appendToExpression(String.valueOf(cardValues[index]));
                    // 禁用该扑克牌
                    disableCard(index);
                    // 标记最后一次点击的是牌
                    lastClickedIsCard = true;
                } else if (!cardUsed[index] && lastClickedIsCard) {
                    showDebouncedToast("请选择符号按钮", Toast.LENGTH_SHORT);
                }
            });
        }
    }

    private void refreshCards() {
        Random random = new Random();
        enableOP();//启用所有符号

        // 先一次性生成四张牌，直到能组成24点（带安全上限避免死循环）
        final int[] candidate = new int[4];
        boolean solvable = false;//初始标记为未找到

        while (!solvable ) {
            for (int i = 0; i < 4; i++) {
                candidate[i] = random.nextInt(10) + 1; // 随机生成1~10四张牌
            }

            // 调用写的 Calculate24 类进行判断
            double[] nums = new double[4];//nums用于计算
            String[] exprs = new String[4];//exprs用于同步存储式子
            for (int i = 0; i < 4; i++) {
                nums[i] = candidate[i];
                exprs[i] = String.valueOf(candidate[i]);
            }

            solvable = Calculate24.js(nums, exprs, 4);
        }

        if (solvable) {
            // 打印找到的表达式到 Logcat
            result=Calculate24.resultExpr;
            System.out.println("Found 24 expression: " + Calculate24.resultExpr);
        }

        // 按原来的动画流程，把 candidate 的图片在动画完成时设置上去
        for (int i = 0; i < cardViews.length; i++) {
            ImageView card = cardViews[i];
            final int index = i; // 需要final用于lambda
            final int newVal = candidate[index];

            // 阶段1：缩小+旋转+淡出（洗牌动作）
            card.animate()
                    .scaleX(0.6f)           // 缩小到60%
                    .scaleY(0.6f)
                    .rotation(360)          // 快速旋转360度
                    .alpha(0.3f)            // 透明度30%
                    .setDuration(350)
                    .setStartDelay(i * 70)  // 依次延迟，有飞出的层次感
                    .withEndAction(() -> {
                        // 阶段2：换牌并生成新数值（这里使用 candidate 而不是每张单独随机）
                        int drawableId = getResources().getIdentifier(
                                "c" + newVal, "drawable", getPackageName()
                        );
                        card.setImageResource(drawableId);
                        // 同步到真正的 cardValues（只有在确定要显示 candidate 时才写入）
                        cardValues[index] = newVal;

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

    private void Tips() {
            etExpression.setText(result);
            // 禁用所有扑克牌
            for(int i=0;i<4;i++)
            {
                disableCard(i);
            }
            //禁用所有符号
            disableOP();
            lastClickedIsCard = false;   // 重置点击状态
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

    //禁用符号按钮
    private void disableOP() {
        findViewById(R.id.btn_add).setAlpha(0.5f); // 半透明
        findViewById(R.id.btn_add).setClickable(false); // 不可点击
        findViewById(R.id.btn_sub).setAlpha(0.5f); // 半透明
        findViewById(R.id.btn_sub).setClickable(false); // 不可点击
        findViewById(R.id.btn_mul).setAlpha(0.5f); // 半透明
        findViewById(R.id.btn_mul).setClickable(false); // 不可点击
        findViewById(R.id.btn_div).setAlpha(0.5f); // 半透明
        findViewById(R.id.btn_div).setClickable(false); // 不可点击
        findViewById(R.id.btn_left).setAlpha(0.5f); // 半透明
        findViewById(R.id.btn_left).setClickable(false); // 不可点击
        findViewById(R.id.btn_right).setAlpha(0.5f); // 半透明
        findViewById(R.id.btn_right).setClickable(false); // 不可点击
    }

    //启用所有扑克牌
    private void enableAllCards() {
        for (int i = 0; i < 4; i++) {
            cardUsed[i] = false;
            cardViews[i].setAlpha(1.0f); // 不透明
            cardViews[i].setClickable(true); // 可点击
        }
    }
    //启用所有符号
    private void enableOP() {
        findViewById(R.id.btn_add).setAlpha(1.0f);
        findViewById(R.id.btn_add).setClickable(true);
        findViewById(R.id.btn_sub).setAlpha(1.0f);
        findViewById(R.id.btn_sub).setClickable(true);
        findViewById(R.id.btn_mul).setAlpha(1.0f);
        findViewById(R.id.btn_mul).setClickable(true);
        findViewById(R.id.btn_div).setAlpha(1.0f);
        findViewById(R.id.btn_div).setClickable(true);
        findViewById(R.id.btn_left).setAlpha(1.0f);
        findViewById(R.id.btn_left).setClickable(true);
        findViewById(R.id.btn_right).setAlpha(1.0f);
        findViewById(R.id.btn_right).setClickable(true);
    }

    private void clear() {
        etExpression.setText(""); // 清空输入框
        enableAllCards(); // 启用所有扑克牌
        enableOP();//启用所有符号
        lastClickedIsCard = false; // 重置牌点击状态

        // 重置Toast防抖状态
        isToastShowing = false;
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
        toastHandler.removeCallbacksAndMessages(null);
    }

    //向输入框追加符号
    private void appendToExpression(String symbol) {
        String currentText = etExpression.getText().toString();
        etExpression.setText(currentText + symbol);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理Toast相关资源，防止内存泄漏
        if (currentToast != null) {
            currentToast.cancel();
        }
        toastHandler.removeCallbacksAndMessages(null);
    }
}