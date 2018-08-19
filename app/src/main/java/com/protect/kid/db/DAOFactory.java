package com.protect.kid.db;

import android.content.Context;

public class DAOFactory {

    public static <T> GeneralDAO<T> getDAO(Context context, Class<T> clazz) {
        BlackWhiteDatabaseHelper dbHelper = BlackWhiteDatabaseHelper.getInstance(context);
        return new GeneralDAO<>(context, dbHelper, clazz);
    }

    public static <T> GeneralDAO<T> getPushDAO(Context context, Class<T> clazz) {
        PushBlackDatabaseHelper dbHelper = PushBlackDatabaseHelper.getInstance(context);
        return new GeneralDAO<>(context, dbHelper, clazz);
    }
}
