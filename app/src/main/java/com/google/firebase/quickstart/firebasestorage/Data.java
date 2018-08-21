package com.google.firebase.quickstart.firebasestorage;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity(indices = {@Index(value = "path", unique = true)})
public class Data {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "stage")
    public int stage = 1;

    @ColumnInfo(name = "is_upload")
    public boolean isUpload = false;

    @ColumnInfo(name = "path")
    public String path;
}
