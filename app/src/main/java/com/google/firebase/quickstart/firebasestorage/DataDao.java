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
    @Query("select * from data order by is_upload")
    LiveData<List<Data>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertData(Data data);

    @Delete
    void deleteDatas(Data... datas);


    @Query("UPDATE data SET is_upload = :is_upload where id = :id")
    void updateUploadStatus(boolean is_upload, int id);
//
//    @Query("DELETE FROM data WHERE id = :userId")
//    abstract void deleteDatas(long userId);

}
