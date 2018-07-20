package com.timedancing.easyfirewall.core.logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class LogDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "log";
    private static final int DB_VERSION = 1;
    public static final String LOG_TABLE_NAME = "log";
    private static final String CREATE_TABLE = "create table log(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "creationDate INTEGER NOT NULL," +
            "log TEXT NOT NULL" +
            ");";
    private static final String DROP_TABLE = "drop table if exists log";

    public LogDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
    }
}
