package com.example.game240;

//沉浸式所需类
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

// 新增的 imports（用于音频与动画）
import android.media.MediaPlayer;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public class MainActivity extends AppCompatActivity {

    //存储扑克牌数值和状态
    private ImageView[] cardViews; // 扑克牌的图片
    private int[] cardValues = {6,6,6,6};//扑克牌的值
    private boolean[] cardUsed = new boolean[4]; // 记录每张牌是否被使用
    private boolean isTips=false;
    private EditText etExpression; //输入框引用
    private JexlEngine jexlEngine;//第三方库计算引擎
    private boolean lastClickedIsCard = false; //记录最后一次是否点击的是扑克牌
    private String result="6+6+6+6";//初始牌解

    //得分变量
    private int score = 0;
    private TextView tvScore;

    // Toast防抖
    private boolean isToastShowing = false; // 标记Toast是否在冷却期
    private Toast currentToast; // 当前显示的Toast对象
    private Handler toastHandler = new Handler(Looper.getMainLooper());
    private static final long TOAST_DEBOUNCE_TIME = 2000; // Toast防抖冷却时间（毫秒）

    // ==== 新增：音乐与动画字段 ====
    private ImageView ivRecord;
    private MediaPlayer bgMusic;
    private ObjectAnimator recordAnimator;
    private boolean isRecordPlaying = false;
    // =============================

    // ========== 最小修改新增：暂停界面核心变量 ==========
    private View pauseView; // 暂停界面View
    private Button btnMenu; // 菜单/暂停按钮
    private boolean isGamePaused = false; // 标记游戏是否暂停
    // ==================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // 新增：适配刘海屏（让内容延伸到刘海区域）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        //EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // ========== 修改WindowInsets监听（去掉系统栏的padding） ==========
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // 原来的代码是适应系统栏，现在隐藏系统栏，所以padding设为0
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        // ========== 新增：隐藏ActionBar + 沉浸式模式代码 ==========
        // 隐藏标题栏（如果有）
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Android 11+ 隐藏状态栏/导航栏（粘性沉浸式）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                // 隐藏状态栏 + 导航栏
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                // 滑动边缘临时显示，无操作自动隐藏
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        // 初始化函数，启动就干的
        findViews();//绑定扑克牌并初始化cardViews数组

        // ====== 新增：设置音乐与旋转唱片（必须在 findViews() 之后，因为需要 iv_record） ======
        setupRecordPlayer();

        // ========== 初始化暂停界面 ==========
        initPauseView();

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


    // 初始化暂停界面核心逻辑 ==========
    private void initPauseView() {

        LayoutInflater inflater = LayoutInflater.from(this);
        pauseView = inflater.inflate(R.layout.pause_menu, null);

        // 添加到Activity根布局（默认隐藏）
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(pauseView);
        pauseView.setVisibility(View.GONE);

        // 绑定暂停界面按钮事件
        Button btnNew = pauseView.findViewById(R.id.btn_new);
        Button btnResume = pauseView.findViewById(R.id.btn_resume);
        Button btnHelp = pauseView.findViewById(R.id.btn_help);
        Button btnExit = pauseView.findViewById(R.id.btn_exit);

        //新游戏
        // 新游戏按钮点击事件
        btnNew.setOnClickListener(v -> {
            // 1. 隐藏暂停界面（视觉清理）
            pauseView.setVisibility(View.GONE);
            isGamePaused = false;

            // 2. 清理资源（避免跳转后音乐/动画/倒计时仍后台运行）
            // 停止并释放背景音乐
            if (bgMusic != null) {
                if (bgMusic.isPlaying()) bgMusic.stop();
                bgMusic.release();
                bgMusic = null;
            }
            // 停止并销毁旋转动画
            if (recordAnimator != null) {
                recordAnimator.cancel();
                recordAnimator = null;
            }

            // 3. 跳转到 start.xml 对应的 Activity
            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            // 清除返回栈，避免用户返回时回到当前游戏界面（提升体验）
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // 4. 关闭当前游戏界面（Time_model）
            finish();
        });


        // 继续游戏
        btnResume.setOnClickListener(v -> {
            pauseView.setVisibility(View.GONE);
            isGamePaused = false;
            // 恢复音乐和动画
            if (bgMusic != null && !bgMusic.isPlaying() && isRecordPlaying) {
                bgMusic.start();
            }
            // 恢复唱片旋转动画
            if (recordAnimator != null && isRecordPlaying) {
                // 如果动画已暂停，先恢复；如果未运行，直接启动
                if (recordAnimator.isPaused()) {
                    recordAnimator.resume();
                } else if (!recordAnimator.isRunning()) {
                    // 保留当前旋转角度，避免重新从0开始
                    float currentRotation = ivRecord.getRotation();
                    recordAnimator.setFloatValues(currentRotation, currentRotation + 360f);
                    recordAnimator.start();
                }
            }
            // 恢复操作
            enableGameOperations();
            //刷新牌面
            refreshCards();

        });

        // 帮助按钮
        btnHelp.setOnClickListener(v -> {
            pauseView.setVisibility(View.GONE);
            isGamePaused = false;

            for(int i=4;i<8;i++)
            {
                int drawableId = getResources().getIdentifier(
                        "help" + i, "drawable", getPackageName()
                );
                cardViews[i-4].setImageResource(drawableId);
            }

            // 恢复音乐和动画
            if (bgMusic != null && !bgMusic.isPlaying() && isRecordPlaying) {
                bgMusic.start();
            }
            // 恢复唱片旋转动画
            if (recordAnimator != null && isRecordPlaying) {
                // 如果动画已暂停，先恢复；如果未运行，直接启动
                if (recordAnimator.isPaused()) {
                    recordAnimator.resume();
                } else if (!recordAnimator.isRunning()) {
                    // 保留当前旋转角度，避免重新从0开始
                    float currentRotation = ivRecord.getRotation();
                    recordAnimator.setFloatValues(currentRotation, currentRotation + 360f);
                    recordAnimator.start();
                }
            }
            findViewById(R.id.iv_record).setClickable(true);
            findViewById(R.id.btn_Refresh).setClickable(true);
            findViewById(R.id.btn_Menu).setClickable(true);

        });

        // 退出游戏
        btnExit.setOnClickListener(v -> finish());
    }

    // ========== 禁用游戏操作（暂停时） ==========
    private void disableGameOperations() {
        // 禁用扑克牌
        for (ImageView card : cardViews) card.setClickable(false);
        // 禁用功能按钮
        findViewById(R.id.btn_clear).setClickable(false);
        findViewById(R.id.btn_Refresh).setClickable(false);
        findViewById(R.id.btn_Tips).setClickable(false);
        findViewById(R.id.btn_submit).setClickable(false);
        findViewById(R.id.iv_record).setClickable(false);
        // 禁用符号按钮
        disableOP();
        // 禁用菜单按钮
        btnMenu.setClickable(false);
    }
    // ==========================================================

    // ========== 最小修改新增：启用游戏操作（恢复时） ==========
    private void enableGameOperations() {
        // 启用未使用的扑克牌
        for (int i = 0; i < cardViews.length; i++) {
            if (!cardUsed[i]) cardViews[i].setClickable(true);
        }
        // 启用功能按钮
        findViewById(R.id.btn_clear).setClickable(true);
        findViewById(R.id.btn_Refresh).setClickable(true);
        findViewById(R.id.btn_Tips).setClickable(true);
        findViewById(R.id.btn_submit).setClickable(true);
        findViewById(R.id.iv_record).setClickable(true);
        // 启用符号按钮
        enableOP();
        // 启用菜单按钮
        btnMenu.setClickable(true);
    }
    // ==========================================================

    private void findViews() {
        etExpression = findViewById(R.id.et_expression);
        tvScore = findViewById(R.id.tv_score);
        //绑定菜单按钮
        btnMenu = findViewById(R.id.btn_Menu);
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
            enableGameOperations();
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

        //菜单按钮点击事件
        btnMenu.setOnClickListener(v -> {
            if (!isGamePaused) {
                isGamePaused = true;
                // 暂停音乐和动画
                if (bgMusic != null && bgMusic.isPlaying()) bgMusic.pause();
                if (recordAnimator != null && recordAnimator.isRunning()) recordAnimator.pause();
                // 禁用操作
                disableGameOperations();
                // 显示暂停界面
                pauseView.setVisibility(View.VISIBLE);
            }
        });
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
        String originalExpression = etExpression.getText().toString().trim();
        if (originalExpression.isEmpty()) {
            showDebouncedToast("请输入计算式", Toast.LENGTH_SHORT);
            return;
        }

        // 检查是否所有牌都已使用
        if (!isAllCardsUsed()) {
            showDebouncedToast("必须使用所有4张牌进行计算", Toast.LENGTH_SHORT);
            return;
        }

        try {
            // 关键：计算前将表达式中的整数替换为浮点数（如 8→8.0、4→4.0）
            // 正则表达式 \d+ 匹配所有整数，替换为 $0.0（$0表示匹配到的数字）
            String floatExpression = originalExpression.replaceAll("\\d+", "$0.0");

            // 1. 创建浮点数表达式的JEXL对象
            JexlExpression jexlExpression = jexlEngine.createExpression(floatExpression);
            // 2. 执行表达式并获取结果
            Object resultObject = jexlExpression.evaluate(null);

            // 确保结果是一个数字
            if (resultObject instanceof Number) {
                double result = ((Number) resultObject).doubleValue();

                // 判断结果是否为24
                //为24且未使用提示按钮
                if (Math.abs(result - 24) < 1e-6&& !isTips) {
                    showDebouncedToast("计算正确！加十分！", Toast.LENGTH_SHORT);
                    score += 10;
                    tvScore.setText("得分：" + score);
                    // 延迟1秒后刷新牌面
                    new Handler().postDelayed(() -> {
                        enableAllCards();
                        enableOP();
                        refreshCards();
                        etExpression.setText("");
                        lastClickedIsCard = false; // 重置牌点击状态
                    }, 1000);
                }
                else if (Math.abs(result - 24) < 1e-6&& isTips) {
                    showDebouncedToast("已使用提示", Toast.LENGTH_SHORT);
                    // 延迟1秒后刷新牌面
                    new Handler().postDelayed(() -> {
                        enableAllCards();
                        enableOP();
                        refreshCards();
                        etExpression.setText("");
                        lastClickedIsCard = false; // 重置牌点击状态
                        isTips=false;
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
            // 打印异常便于调试（可选）
            e.printStackTrace();
            if (e instanceof ArithmeticException) {
                showDebouncedToast("算式包含除零运算", Toast.LENGTH_SHORT);
            } else {
                showDebouncedToast("算式不合法" , Toast.LENGTH_SHORT);
            }
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
        isTips=false;

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
        isTips=true;
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

    // ======= 新增方法：初始化 MediaPlayer 与 旋转动画，并绑定 iv_record 点击事件 =======
    private void setupRecordPlayer() {
        // iv_record 在布局中已存在（third_container），在此处绑定
        ivRecord = findViewById(R.id.iv_record);

        // 初始化 MediaPlayer（确保文件位于 res/raw/bg_music.mp3）
        try {
            bgMusic = MediaPlayer.create(this, R.raw.bg_music);
            if (bgMusic != null) {
                bgMusic.setLooping(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            bgMusic = null;
        }

        // 使用 ObjectAnimator 实现匀速无限旋转 (7s 一周，与网页中一致)
        recordAnimator = ObjectAnimator.ofFloat(ivRecord, "rotation", 0f, 360f);
        recordAnimator.setDuration(7000); // 7 秒一圈
        recordAnimator.setRepeatCount(ValueAnimator.INFINITE);
        recordAnimator.setInterpolator(new LinearInterpolator());

        // 点击切换播放 / 暂停 与动画
        ivRecord.setOnClickListener(v -> {
            if (bgMusic == null) return; // 安全检查

            if (isRecordPlaying) {
                // 暂停音乐并停止动画
                try {
                    if (bgMusic.isPlaying()) bgMusic.pause();
                } catch (IllegalStateException ignored) {}
                recordAnimator.cancel();
                // 可选：重置角度到 0（与网页移除 class 后无旋转效果一致）
                ivRecord.setRotation(0f);
            } else {
                // 播放音乐并启动动画
                try {
                    bgMusic.start();
                } catch (IllegalStateException ex) {
                    ex.printStackTrace();
                }
                recordAnimator.start();
            }
            isRecordPlaying = !isRecordPlaying;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停音乐与动画（进入后台时）
        if (bgMusic != null && bgMusic.isPlaying()) {
            try { bgMusic.pause(); } catch (IllegalStateException ignored) {}
        }
        if (recordAnimator != null && recordAnimator.isRunning()) {
            recordAnimator.cancel();
        }
        isRecordPlaying = false;

        // 若你需要在暂停时也停止倒计时，可在此处取消 countDownTimer（根据需求）
        // if (countDownTimer != null) countDownTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理MediaPlayer资源，防止内存泄漏
        if (bgMusic != null) {
            try {
                if (bgMusic.isPlaying()) bgMusic.stop();
            } catch (Exception ignored) {}
            bgMusic.release();
            bgMusic = null;
        }

        // 取消动画
        if (recordAnimator != null) {
            recordAnimator.cancel();
            recordAnimator = null;
        }

        // 清理Toast相关资源，防止内存泄漏（保留你原有逻辑）
        if (currentToast != null) {
            currentToast.cancel();
        }
        toastHandler.removeCallbacksAndMessages(null);
    }
}
