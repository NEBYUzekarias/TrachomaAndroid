package com.google.firebase.quickstart.firebasestorage;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.google.firebase.quickstart.firebasestorage.DataDao;

@Database(entities = {Data.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DataDao dataDao();

    private static AppDatabase db = null;

    public static AppDatabase getDb(Context context){
        if (db == null) {
            synchronized (AppDatabase.class){
                if (db == null) {
                    db = Room.databaseBuilder(context, AppDatabase.class, "data")
                            .build();
                }
            }
        }

        return db;
    }

    public static void destoryDb() {
        db = null;
    }
}
