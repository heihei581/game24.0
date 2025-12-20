package com.example.game240;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库配置
    private static final String DB_NAME = "Game24DB";
    private static final int DB_VERSION = 1;
    // 表名 + 字段
    private static final String TABLE_RECORDS = "tips_records";
    private static final String COLUMN_ID = "id"; // 主键（自增）
    private static final String COLUMN_CARDS = "cards"; // 牌面（如：3 4 5 6）
    private static final String COLUMN_ANSWER = "answer"; // 答案公式（如：(3+5-4)×6=24）
    private static final String COLUMN_TIME = "create_time"; // 时间戳（用于排序）

    // 最大记录数（200条）
    private static final int MAX_RECORDS = 200;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // 创建表
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_RECORDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CARDS + " TEXT NOT NULL, " +
                COLUMN_ANSWER + " TEXT NOT NULL, " +
                COLUMN_TIME + " INTEGER DEFAULT (strftime('%s', 'now')))"; // 时间戳（秒）
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级时删除旧表（简单处理，实际项目可做数据迁移）
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        onCreate(db);
    }

    // 插入提示记录（点击提示时调用）
    public void insertTipRecord(String cards, String answer) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            // 1. 先检查记录数，超过200条则删除最旧的
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECORDS, null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                if (count >= MAX_RECORDS) {
                    // 删除id最小的（最旧）记录
                    db.execSQL("DELETE FROM " + TABLE_RECORDS +
                            " WHERE " + COLUMN_ID + " = (SELECT MIN(" + COLUMN_ID + ") FROM " + TABLE_RECORDS + ")");
                }
            }
            cursor.close();

            // 2. 插入新记录
            ContentValues values = new ContentValues();
            values.put(COLUMN_CARDS, cards);
            values.put(COLUMN_ANSWER, answer);
            db.insert(TABLE_RECORDS, null, values);
        } catch (Exception e) {
            Log.e("DBHelper", "插入记录失败：" + e.getMessage());
        } finally {
            db.close();
        }
    }

    // 查询所有提示记录（按时间倒序，最新的在前）
    public List<Record> queryAllTipRecords() {
        List<Record> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            // 查询并按时间倒序排序
            cursor = db.query(TABLE_RECORDS,
                    new String[]{COLUMN_CARDS, COLUMN_ANSWER},
                    null, null, null, null,
                    COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                String cards = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARDS));
                String answer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER));
                records.add(new Record(cards, answer));
            }
        } catch (Exception e) {
            Log.e("DBHelper", "查询记录失败：" + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return records;
    }

    // 数据模型：封装单条记录
    public static class Record {
        public String cards;
        public String answer;

        public Record(String cards, String answer) {
            this.cards = cards;
            this.answer = answer;
        }
    }
}