package com.timedancing.easyfirewall.core.logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class Logger {
    private LogDbHelper mDbHelper;
    private static Logger sInstance;

    public synchronized static Logger getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Logger(context.getApplicationContext());
        }
        return sInstance;
    }

    public Logger(Context context) {
        mDbHelper = new LogDbHelper(context);
    }

    public void insert(String log) {
        ContentValues cv = new ContentValues();
        cv.put("creationDate", System.currentTimeMillis());
        cv.put("log", log);
        mDbHelper.getWritableDatabase().insert(LogDbHelper.LOG_TABLE_NAME, null, cv);
    }

    public Cursor getAll(boolean isDesc) {
        String order = isDesc ? "creationDate desc" : "creationDate asc";
        return mDbHelper.getReadableDatabase().query(LogDbHelper.LOG_TABLE_NAME, null,
                null, null, null, null, order);
    }

    public Cursor find(String filter, boolean isDesc) {
        if (TextUtils.isEmpty(filter)) {
            return getAll(isDesc);
        }
        String order = isDesc ? "creationDate desc" : "creationDate asc";
        return mDbHelper.getReadableDatabase().query(LogDbHelper.LOG_TABLE_NAME, null,
                "log like '%" + filter + "%'", null, null, null, order);
    }

    public void clear() {
        mDbHelper.getWritableDatabase().delete(LogDbHelper.LOG_TABLE_NAME, null, null);
    }
}
