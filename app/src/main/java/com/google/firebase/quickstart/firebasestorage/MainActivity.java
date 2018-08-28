/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.quickstart.firebasestorage;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

//import com.google.firebase.auth.AuthResult;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;

/**
 * Activity to upload and download photos from Firebase Storage.
 *
 * See {@link MyUploadService} for upload example.
 * See {@link MyDownloadService} for download example.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {


// Recycle view
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public  CountDrawable badge;
    private LayerDrawable icon;

    private static final String TAG = "Storage#MainActivity";

    private static final int RC_TAKE_PICTURE = 101;

    public static final String DATA_ID = "data_id";
    public static final String DATA_STAGE = "data_stage";

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";

    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mProgressDialog;
    private Drawable reuse;
    private int count;
    //private FirebaseAuth mAuth;
    ImageButton floatButton;
    View upload;
    public CountDrawable countDrawable;

    private Uri mDownloadUrl = null;
    private Uri mFileUri = null;

    private DataViewModel mDataViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start RecycleView
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        RecyclerViewClickListener listener = (view, data) -> {
            Uri uri;
            if (data.path.startsWith("file:///")) {
                File file = new File(data.path.substring(8));
                uri = Uri.fromFile(file);
            } else {
                uri = Uri.parse(data.path);
            }

            uploadFromUri(uri , data);
        };
        MyAdapter.ButtonListener buttonListener = new MyAdapter.ButtonListener() {
            @Override
            public void deleteOnClick(View v, Data position) {


                mDataViewModel.deleteData(position);
            }
        };
        mAdapter = new MyAdapter(listener , buttonListener);
        mRecyclerView.setAdapter(mAdapter);
        countDrawable  = new CountDrawable();

        // end of RecycleView

        // get view model for data
        mDataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);
        // assign observer for possible data change
        mDataViewModel.getAllData().observe(this, new Observer<List<Data>>() {
            @Override
            public void onChanged(@Nullable List<Data> datas) {
                // Update the cached copy of the data in the adapter.
                mAdapter.setDatas(datas);

//                if (reuse != null && reuse instanceof CountDrawable) {
//                    badge = (CountDrawable) reuse;
//                } else {
//                    badge = new CountDrawable(getApplicationContext());
//                }

                count = datas.size();
                if (!datas.isEmpty()) {
                    for (Data data : datas) {
                        if (data.isUpload) {
                             count = count - 1;
                        }
                    }
                }
                if(icon!=null)
                setCount(getApplicationContext(), Integer.toString(count),icon  );
            }
        });

        // Initialize Firebase Auth
       // mAuth = FirebaseAuth.getInstance();

        // Click listeners

        //floating button
        floatButton = (ImageButton) findViewById(R.id.imageButton);
        floatButton.setOnClickListener(this);


        // Restore instance state
        if (savedInstanceState != null) {
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            mDownloadUrl = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
        }
        onNewIntent(getIntent());

        // Local broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);
                hideProgressDialog();

                switch (intent.getAction()) {

                    case MyUploadService.UPLOAD_COMPLETED:
                    case MyUploadService.UPLOAD_ERROR:
                        onUploadResultIntent(intent);
                        break;
                }
            }
        };
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(MyUploadService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
       // updateUI(mAuth.getCurrentUser());

        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mBroadcastReceiver, MyDownloadService.getIntentFilter());
        manager.registerReceiver(mBroadcastReceiver, MyUploadService.getIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable(KEY_FILE_URI, mFileUri);
        out.putParcelable(KEY_DOWNLOAD_URL, mDownloadUrl);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
//        if (requestCode == RC_TAKE_PICTURE) {
//            if (resultCode == RESULT_OK) {
//                mFileUri = data.getData();
//
//
//                if (mFileUri != null) {
//                    uploadFromUri(mFileUri);
//                } else {
//                    Log.w(TAG, "File URI is null");
//                }
//            } else {
//                Toast.makeText(this, "Taking picture failed.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private void uploadFromUri(Uri fileUri, Data data) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // Save the File URI
        mFileUri = fileUri;

        // Clear the last download, if any
      //  updateUI(mAuth.getCurrentUser());
        mDownloadUrl = null;

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Activity is killed or put in the background
        ComponentName componentName = startService(new Intent(this, MyUploadService.class)
                .putExtra(MyUploadService.EXTRA_FILE_URI, fileUri)
                .putExtra(DATA_ID, data.id)
                .putExtra(DATA_STAGE, data.stage)
                .setAction(MyUploadService.ACTION_UPLOAD));

        // Show loading spinner
        showProgressDialog(getString(R.string.progress_uploading));
    }



    private void launchCamera() {
        Log.d(TAG, "launchCamera");

        // Pick an image from storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, RC_TAKE_PICTURE);
    }


    private void onUploadResultIntent(Intent intent) {
        // Got a new intent from MyUploadService with a success or failure
        mDownloadUrl = intent.getParcelableExtra(MyUploadService.EXTRA_DOWNLOAD_URL);
        mFileUri = intent.getParcelableExtra(MyUploadService.EXTRA_FILE_URI);


    }


    private void showProgressDialog(String caption) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(caption);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

            return super.onOptionsItemSelected(item);

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (i == R.id.upload){

            launchCamera();

        }
        else if (i == R.id.imageButton) {
            Intent open_collector = new Intent(this, DataCollector.class);
            startActivity(open_collector);
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.ic_group);
        icon = (LayerDrawable) menuItem.getIcon();
        count = 0;
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                List<Data> datas = mDataViewModel.getAllData().getValue();
                if (!datas.isEmpty()) {

                    for (Data data : datas) {
                        if (data.isUpload) {


                            Toast.makeText(getApplicationContext(), "Already uploaded", Toast.LENGTH_LONG)
                                            .show();



                        } else {
                            Uri uri;
                            if (data.path.startsWith("file:///")) {
                                File file = new File(data.path.substring(8));
                                uri = Uri.fromFile(file);
                            } else {
                                uri = Uri.parse(data.path);
                            }

                            uploadFromUri(uri , data);
                        }
                    }
                }

                return true;}
        });
        List<Data> datas = mDataViewModel.getAllData().getValue();


        setCount(getApplicationContext(), Integer.toString(this.count),icon  );




        return true;
    }


    public void setCount(Context context, String count , LayerDrawable icon) {
//        if (!datas.isEmpty()){
//            for(int i=0 ; i<=(datas.size()-1) ; i ++){
//        Data dataa = datas.get(i);
//        if (dataa.isUpload) {
//            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()  {
//                @Override
//                public boolean onMenuItemClick(MenuItem menuItem) {
//
//                    Toast.makeText(context, "Already uploaded", Toast.LENGTH_LONG)
//                            .show();
//
//                    return true;
//                }
//            });
//        }
//else {
//        // adding listener to menuItem
//        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem menuItem) {
//                Uri uri;
//                if (dataa.path.startsWith("file:///")) {
//                    File file = new File(dataa.path.substring(8));
//                    uri = Uri.fromFile(file);
//                } else {
//                    uri = Uri.parse(dataa.path);
//                }
//
//                uploadFromUri(uri , dataa);
//                return true;
//            }
//        });}}}


        // Reuse drawable if possible
         reuse = icon.findDrawableByLayerId(R.id.ic_group_count);
        if (reuse != null && reuse instanceof CountDrawable) {
            badge = (CountDrawable) reuse;
        } else {
            badge = new CountDrawable(context);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_group_count, badge);
    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuItem item = menu.findItem(R.id.back_item);
//        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                YourActivity.this.someFunctionInYourActivity();
//                return true;
//            }
//        });
//        return true;
//    }
}
