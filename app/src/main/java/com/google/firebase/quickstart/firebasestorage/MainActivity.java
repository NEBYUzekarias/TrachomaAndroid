/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.quickstart.firebasestorage;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.support.v4.util.Pair;
import java.io.File;
import java.util.List;

import static android.widget.Toast.makeText;

/**
 * Activity to upload and download photos from Firebase Storage.
 * <p>
 * See {@link MyUploadService} for upload example.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener ,DeleteDialogFragment.NoticeDialogListener {


    // Recycle view
//    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public CountDrawable badge;
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

    private final int imageWidthPixels = 1024;
    private final int imageHeightPixels = 768;
    //private FirebaseAuth mAuth;
    ImageButton floatButton;
    View upload;
    public CountDrawable countDrawable;
    public Data position;

    private Uri mDownloadUrl = null;
    private Uri mFileUri = null;

    private DataViewModel mDataViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start RecycleView
      EmptyRecyclerView  mRecyclerView ;
      mRecyclerView =(EmptyRecyclerView)findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(20);
        mRecyclerView.setDrawingCacheEnabled(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        View emptyView = findViewById(R.id.todo_list_empty_view);
        mRecyclerView.setEmptyView(emptyView);

        RecyclerViewClickListener listener = (view, data , id) -> {
            if ( id == R.id.upload){

                if (isOnline()) {
                    Uri uri;
                    if (data.path.startsWith("file:///")) {
                        File file = new File(data.path.substring(8));
                        uri = Uri.fromFile(file);
                    } else {
                        uri = Uri.parse(data.path);
                    }

                    uploadFromUri(uri, data);

                }
                else {

                    Log.d("internat", "internat: no internat connection");


                    Toast toast =  makeText(getApplicationContext(),"no internate" , Toast.LENGTH_SHORT);

                    toast.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);

                    toast.show();

                }
            }

            else {



                Intent intent = new Intent(this, DetailActivity.class);
// Pass data object in the bundle and populate details activity.
                Pair<View, String> p1 = Pair.create((View)findViewById(R.id.im_stage), "image");

                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(this,p1);
                startActivity(intent, options.toBundle());
//                startActivity(new Intent(this, DetailActivity.class));


            }
        };

        MyAdapter.ButtonListener buttonListener = new MyAdapter.ButtonListener() {
            @Override
            public void deleteOnClick(View v, Data position1) {
                position = position1;
                showNoticeDialog();
//                mDataViewModel.deleteData(position);
            }
        };






        mAdapter = new MyAdapter(listener, buttonListener);
        mRecyclerView.setAdapter(mAdapter);
        countDrawable = new CountDrawable();

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

                count = 0;
                if (datas != null) {
                    for (Data data : datas) {
                        if (!data.isUpload) {
                            count = count + 1;
                        }
                    }
                } else {
                    Log.d("data", "datas: null");
                }
                if (icon != null)
                    setCount(getApplicationContext(), Integer.toString(count), icon);
            }
        });

        // Click listeners

        //floating button
        floatButton = (ImageButton) findViewById(R.id.add_fab_btn);
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

        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
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
        switch (item.getItemId()) {
            case R.id.info:
                startActivity(new Intent(this, InfoActivity.class));
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (i == R.id.add_fab_btn) {
            Intent open_collector = new Intent(this, DataCollector.class);
            startActivity(open_collector);
        }
        else{

                startActivity(new Intent(this, DetailActivity.class));
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.upload_all);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                List<Data> datas = mDataViewModel.getAllData().getValue();

                if (datas != null) {
                    for (Data data : datas) {
                        if (data.isUpload) {
                            makeText(getApplicationContext(),
                                    "Already uploaded2", Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            Uri uri;
                            if (data.path.startsWith("file:///")) {
                                File file = new File(data.path.substring(8));
                                uri = Uri.fromFile(file);
                            } else {
                                uri = Uri.parse(data.path);
                            }

                            uploadFromUri(uri, data);
                        }
                    }
                } else {
                    Log.d("data", "datas: null");
                }

                return true;
            }
        });

        icon = (LayerDrawable) menuItem.getIcon();

        List<Data> datas = mDataViewModel.getAllData().getValue();
        count = 0;
        if (datas != null) {
            for (Data data: datas){
                if (!data.isUpload){
                    count = count + 1;
                }
            }
        } else {
            Log.d("data", "datas: null");
        }
        setCount(getApplicationContext(), Integer.toString(this.count), icon);

        return true;
    }


    public void setCount(Context context, String count, LayerDrawable icon) {

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

    public void showNoticeDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new DeleteDialogFragment();
        dialog.show(getFragmentManager(),"DeleteDialogFragment");
    }


    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }



    public void onDialogPositiveClick(DialogFragment dialog ) {
        // User touched the dialog's positive button


        mDataViewModel.deleteData(position);

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    }

}
