package com.google.firebase.quickstart.firebasestorage;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class DataViewModel extends AndroidViewModel {
    private DataRepository mRepository;
    private LiveData<List<Data>> mAllData;

    public DataViewModel(Application application){
        super(application);

        mRepository = new DataRepository(application);
        mAllData = mRepository.getAllData();
    }

    LiveData<List<Data>> getAllData() { return mAllData; }

    public void insertData(Data data) { mRepository.insert(data); }

}
