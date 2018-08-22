package com.google.firebase.quickstart.firebasestorage;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface DataDao {
    @Query("select * from data")
    LiveData<List<Data>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertData(Data data);

    @Update
    void updateData(Data data);

    @Update
    void updateDatas(Data... datas);

    @Delete
    void deleteDatas(Data... datas);
}
