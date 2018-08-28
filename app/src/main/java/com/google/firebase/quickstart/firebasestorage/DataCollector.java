package com.google.firebase.quickstart.firebasestorage;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataCollector extends AppCompatActivity implements View.OnClickListener {

    private final int GET_SINGLE_IMAGE = 1;
    private final int CAPTURE_IMAGE = 2;

    private final int REQUEST_WRITE_EXTERNAL = 1;

    private LinearLayout mStagesView;
    private RadioGroup mStageRadios;
    private RadioButton[] mRadios;
    private List<CardView> mCards;
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

        mRadios = new RadioButton[]{
                (RadioButton) findViewById(R.id.radio_1),
                (RadioButton) findViewById(R.id.radio_2),
                (RadioButton) findViewById(R.id.radio_3),
                (RadioButton) findViewById(R.id.radio_4),
                (RadioButton) findViewById(R.id.radio_5),
        };

        mCards = new ArrayList<>();
        mCards.add((CardView) findViewById(R.id.card_1));
        mCards.add((CardView) findViewById(R.id.card_2));
        mCards.add((CardView) findViewById(R.id.card_3));
        mCards.add((CardView) findViewById(R.id.card_4));
        mCards.add((CardView) findViewById(R.id.card_5));

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
//            Uri photoURI = FileProvider.getUriForFile(this,
//                    "com.example.android.fileprovider",
//                    photoFile);
            Uri photoURI = Uri.fromFile(photoFile);
            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePicture, CAPTURE_IMAGE);
        } else {
            Log.i("photo", "photo: null");
        }
    }

    private File createImageFile() throws IOException {
        File image = null;
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "DATA_" + timeStamp + "_";

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL);
        } else {
            Log.i("my", "permission granted");
            // handle storage direcotory
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            storageDir.mkdirs();

            // create temp file
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file path for use when returning to this Activity
            mCapturedPhotoPath = image.getAbsolutePath();
        }

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
            int radio_id = view.getId();
            setRadio(radio_id);
            switch (radio_id) {
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

    // set all radio buttons unchecked when one is selected
    public void setRadio(int radio_id) {
        for (RadioButton radioButton: mRadios) {
            radioButton.setChecked(radio_id == radioButton.getId());
        }
    }

    public void onCardRadioButtonClicked(View view) {
            int card_id = view.getId();
            setCardRadio((CardView) view);
            switch (card_id) {
                case R.id.card_1:
                    mData.stage = 1;
                    break;
                case R.id.card_2:
                    mData.stage = 2;
                    break;
                case R.id.card_3:
                    mData.stage = 3;
                    break;
                case R.id.card_4:
                    mData.stage = 4;
                    break;
                case R.id.card_5:
                    mData.stage = 5;
                    break;
            }

    }

    // set all radio buttons unchecked when one is selected
    public void setCardRadio(CardView cardView) {
        int index = mCards.indexOf(cardView);

        for (int i=0;i < mRadios.length;i++) {
            mRadios[i].setChecked(index == i);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast toast = Toast.makeText(this,
                            "You can take picture now.",
                            Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(this,
                            "You have to grant permission to take picture.",
                            Toast.LENGTH_LONG);
                    toast.show();
                }

                return;
        }
    }
}
