package com.google.firebase.quickstart.firebasestorage;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class DataRepository {
    private DataDao mDataDao;
    private LiveData<List<Data>> mAllData;

    DataRepository(Application application) {
        AppDatabase db = AppDatabase.getDb(application);
        mDataDao = db.dataDao();
        mAllData = mDataDao.getAll();
    }

    LiveData<List<Data>> getAllData(){
        return mAllData;
    }

    public void insert(Data data){
        new InsertAyncTask(mDataDao).execute(data);
    }

    private static class InsertAyncTask extends AsyncTask<Data, Void, Void> {
        private DataDao mAsyncTaskDao;

        InsertAyncTask(DataDao dataDao) {
            mAsyncTaskDao = dataDao;
        }

        @Override
        protected Void doInBackground(Data... datas) {
            mAsyncTaskDao.insertData(datas[0]);
            return null;
        }
    }
}
