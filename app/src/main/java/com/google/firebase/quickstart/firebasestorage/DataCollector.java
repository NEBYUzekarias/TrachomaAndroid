package com.google.firebase.quickstart.firebasestorage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class DataCollector extends AppCompatActivity implements View.OnClickListener {

    private int GET_SINGLE_IMAGE = 1;

    private ImageView selectedImage;
    private Uri selectedUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collector);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up button listeners
        findViewById(R.id.add_btn).setOnClickListener(this);
        findViewById(R.id.gallery_btn).setOnClickListener(this);

        selectedImage = (ImageView) findViewById(R.id.selected_image);
    }

    @Override
    public void onClick(View view) {
        int view_id = view.getId();
        switch (view_id) {
            case R.id.gallery_btn:
                chooseFromGallery();
                break;
            case R.id.add_btn:
                if (selectedUri != null){
                    storeDataDetails(selectedUri);
                } else {
                    Snackbar.make(view, "No Picture Selected", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                break;
        }
    }

    private void chooseFromGallery() {
        Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
        getContent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(getContent, "Select picture"),
                GET_SINGLE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == GET_SINGLE_IMAGE && data != null){
                Uri uri = data.getData();
                //Todo: may most probably need to process to get uri that is file path, but who cares
                if (uri != null) {
                    Log.i("uri", String.format("uri: %s", uri.toString()));

                    try {
                        Bitmap bitmap =
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                        selectedImage.setImageBitmap(bitmap);
                        selectedUri = uri;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i("uri", "uri: null");
                }
            }
        }
    }

    public void storeDataDetails(Uri uri) {
        Data data = new Data();
        data.path = uri.toString();

        AppDatabase db = AppDatabase.getDb(getApplicationContext());

        // insert data on background thread, cant insert on main thread
        new InsertAyncTask(db.dataDao()).execute(data);

        new GetAllAyncTask(db.dataDao()).execute();

    }

    private static class InsertAyncTask extends AsyncTask<Data, Void, Void> {
        private DataDao asyncTaskDao;

        InsertAyncTask(DataDao dataDao) {
            asyncTaskDao = dataDao;
        }

        @Override
        protected Void doInBackground(Data... data) {
            asyncTaskDao.insertData(data[0]);
            return null;
        }
    }

    private static class GetAllAyncTask extends AsyncTask<Void, Void, List<Data>> {
        private DataDao asyncTaskDao;

        GetAllAyncTask(DataDao dataDao) {
            asyncTaskDao = dataDao;
        }

        @Override
        protected List<Data> doInBackground(Void... voids) {
            return asyncTaskDao.getAll();
        }

        @Override
        protected void onPostExecute(List<Data> datas) {
            for (Data datum: datas) {
                Log.i("datum", String.format("datum: %s", datum));
            }
        }
    }
}
