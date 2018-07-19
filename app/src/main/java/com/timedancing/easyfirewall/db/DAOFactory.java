package com.timedancing.easyfirewall.db;

import android.content.Context;

import com.timedancing.easyfirewall.util.GeneralDAO;

public class DAOFactory {

    public static <T> GeneralDAO<T> getDAO(Context context, Class<T> clazz) {
        BlackWhiteDatabaseHelper dbHelper = BlackWhiteDatabaseHelper.getInstance(context);
        return new GeneralDAO<>(context, dbHelper, clazz);
    }
}
