package com.timedancing.easyfirewall.core.blackwhite;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "white_ip")
public class WhiteIP implements StringItem {
    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(columnName = "content")
    public String ip;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getText() {
        return ip;
    }
}