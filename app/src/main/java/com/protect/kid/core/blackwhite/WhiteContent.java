package com.protect.kid.core.blackwhite;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "white_content")
public class WhiteContent implements StringItem {
    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(columnName = "content")
    public String content;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getText() {
        return content;
    }
}
