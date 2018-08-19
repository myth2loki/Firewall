package com.protect.kid.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.protect.kid.core.blackwhite.BlackContent;
import com.protect.kid.core.blackwhite.BlackIP;
import com.protect.kid.core.blackwhite.WhiteContent;
import com.protect.kid.core.blackwhite.WhiteIP;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class PushBlackDatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TABLE_NAME = "push_black.db";
    private static PushBlackDatabaseHelper instance;
    private Map<String, Dao> daoMap = new HashMap<>();

    private PushBlackDatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    /**
     * 单例模式
     *
     * @param context
     * @return
     */
    public static PushBlackDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (PushBlackDatabaseHelper.class) {
                if (instance == null) {
                    instance = new PushBlackDatabaseHelper(context);
                }
            }
        }
        return instance;
    }

    /**
     * 建表
     *
     * @param database
     * @param connectionSource
     */
    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            //建立表user_table
            TableUtils.createTable(connectionSource, BlackIP.class);
            TableUtils.createTable(connectionSource, BlackContent.class);
            TableUtils.createTable(connectionSource, WhiteIP.class);
            TableUtils.createTable(connectionSource, WhiteContent.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * 更新
     *
     * @param database
     * @param connectionSource
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, BlackIP.class, true);
            TableUtils.dropTable(connectionSource, BlackContent.class, true);
            TableUtils.dropTable(connectionSource, WhiteIP.class, true);
            TableUtils.dropTable(connectionSource, WhiteContent.class, true);
            onCreate(database, connectionSource);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Dao类，通过Dao类可以对表里的数据进行增删改查
     *
     * @param clazz
     * @return
     * @throws SQLException
     */
    public synchronized Dao getDao(Class clazz) throws SQLException {
        Dao dao = null;
        String className = clazz.getSimpleName();
        if (daoMap.containsKey(className)) {
            dao = daoMap.get(className);
        }
        if (dao == null) {
            dao = super.getDao(clazz);
            daoMap.put(className, dao);
        }
        return dao;
    }

    /**
     * 清除某个表内的数据
     *
     * @param clazz
     */
    public synchronized void clearData(Class clazz) {
        try {
            TableUtils.clearTable(connectionSource, clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}