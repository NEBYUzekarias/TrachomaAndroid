package com.google.firebase.quickstart.firebasestorage;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataCollector extends AppCompatActivity implements View.OnClickListener {

    private final int GET_SINGLE_IMAGE = 1;
    private final int CAPTURE_IMAGE = 2;

    private LinearLayout mStagesView;
    private RadioGroup mStageRadios;
    private ImageView mSelectedImage;
    private Uri mSelectedUri;

    private DataViewModel mDataViewModel;
    private Data mData;

    private boolean isCaptureMode;
    private String mCapturedPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collector);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up button listeners
        findViewById(R.id.add_btn).setOnClickListener(this);
        findViewById(R.id.gallery_btn).setOnClickListener(this);
        findViewById(R.id.capture_btn).setOnClickListener(this);

        mSelectedImage = (ImageView) findViewById(R.id.selected_image);
        mStageRadios = (RadioGroup) findViewById(R.id.stage_radios);
        mStagesView = (LinearLayout) findViewById(R.id.stages_view);
//        mStagesView.setVisibility(View.GONE);

        // get view model for data
        mDataViewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        // create fresh data to save details
        mData = new Data();
    }

    private void chooseFromGallery() {
        Intent getContent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        getContent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(getContent, "Select picture"),
                GET_SINGLE_IMAGE);
    }

    private void captureImage() {
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            // Error occurred while creating the File
            Log.i("file_creation", "could not create file");
            e.printStackTrace();
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    photoFile);
            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePicture, CAPTURE_IMAGE);
        } else {
            Log.i("photo", "photo: null");
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "DATA_" + timeStamp + "_";

        // handle storage direcotory
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        storageDir.mkdirs();

        // create temp file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file path for use when returning to this Activity
        mCapturedPhotoPath = image.getAbsolutePath();
        return image;
    }

    public boolean storeDataDetails() {
        if (mData.stage == 0){
            Snackbar.make(mSelectedImage, "No Stage Selected", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            return false;
        } else {
            if (mSelectedUri != null){
                mData.path = mSelectedUri.toString();

                // insert data only when it is valid, otherwise notify users
                mDataViewModel.insertData(mData);
                return true;
            } else {
                Snackbar.make(mSelectedImage, "No Picture Selected", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                return false;
            }
        }
    }

    public void resetState() {
        mSelectedUri = null;
        mCapturedPhotoPath = null;
        mData = new Data();

        mSelectedImage.setImageResource(R.drawable.gallery);
        mStageRadios.clearCheck();
    }

    public void showStagesArea() {
        // drop down stages selection view
        Animation slide_down = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_down);
        // mStagesView.startAnimation(slide_down);
        mStagesView.setVisibility(View.VISIBLE);
        // mStagesView.setAlpha(0.0f);
        mStagesView.animate()
                .translationY(mStagesView.getHeight())
        //      .alpha(1.0f)
                .setListener(null);
    }

    public void hideStagesArea() {
        // drop down stages selection view
        Animation slide_up = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_up);
        mStagesView.animate()
                .translationY(0)
        //      .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mStagesView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onClick(View view) {
        int view_id = view.getId();

        switch (view_id) {
            case R.id.gallery_btn:
                isCaptureMode = false;
                chooseFromGallery();
                break;
            case R.id.capture_btn:
                isCaptureMode = true;
                captureImage();
                break;
            case R.id.add_btn:
                boolean detailsStored = storeDataDetails();
                if (detailsStored){
                    resetState();
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("result", "in result");
        if (resultCode == RESULT_OK) {
            Log.i("result", "ok code: " + requestCode);
            if (requestCode == GET_SINGLE_IMAGE && data != null) {
                Uri uri = data.getData();
                //Todo: may most probably need to process to get uri that is file path, but who cares
                if (uri != null) {
                    Log.i("uri", String.format("uri: %s", uri.toString()));

                    try {
                        Bitmap bitmap =
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                        mSelectedImage.setImageBitmap(bitmap);
                        mSelectedUri = uri;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i("uri", "uri: null");
                }
            } else if (requestCode == CAPTURE_IMAGE) {
                if (mCapturedPhotoPath != null) {
                    File f = new File(mCapturedPhotoPath);
                    Uri uri = Uri.fromFile(f);

                    mSelectedUri = uri;
                    mSelectedImage.setImageURI(uri);
                }
            }
        } else {
            Log.i("result", "not ok");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:

                return true;
            case R.id.setup:

                return true;
            case R.id.about:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        if (checked) {
            switch (view.getId()) {
                case R.id.radio_1:
                    mData.stage = 1;
                    break;
                case R.id.radio_2:
                    mData.stage = 2;
                    break;
                case R.id.radio_3:
                    mData.stage = 3;
                    break;
                case R.id.radio_4:
                    mData.stage = 4;
                    break;
                case R.id.radio_5:
                    mData.stage = 5;
                    break;
            }
        }

    }
}
