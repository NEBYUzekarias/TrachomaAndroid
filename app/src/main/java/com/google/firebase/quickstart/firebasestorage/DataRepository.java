package com.google.firebase.quickstart.firebasestorage;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

public class DataRepository {
    private DataDao mDataDao;
    private LiveData<List<Data>> mAllData;

    DataRepository(Context application) {
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



    public void delete(Data data){
        new DeleteAsyncTask(mDataDao).execute(data);
    }
    private static class DeleteAsyncTask extends AsyncTask<Data, Void, Void> {
        private DataDao mAsyncTaskDao;

        DeleteAsyncTask(DataDao dataDao) {
            mAsyncTaskDao = dataDao;
        }

        @Override
        protected Void doInBackground(Data... datas) {
            mAsyncTaskDao.deleteDatas(datas[0]);
            return null;
        }
    }



    public void updateUploadStatus(boolean is_upload, int data_id) {
        new UpdateUploadStatusTask(mDataDao).execute(new UpdateParams(data_id, is_upload));
    }

    public static class UpdateParams {
        int id;
        boolean is_upload;

        public UpdateParams(int id, boolean is_upload) {
            this.id = id;
            this.is_upload = is_upload;
        }
    }

    private static class UpdateUploadStatusTask extends AsyncTask<UpdateParams, Void, Void> {
        private DataDao mAsyncTaskDao;

        UpdateUploadStatusTask(DataDao dataDao) {
            mAsyncTaskDao = dataDao;
        }

        @Override
        protected Void doInBackground(UpdateParams... params) {
            mAsyncTaskDao.updateUploadStatus(params[0].is_upload, params[0].id);
            return null;
        }
    }
}
