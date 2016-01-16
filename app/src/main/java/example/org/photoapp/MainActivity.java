package example.org.photoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Displays two buttons to the user, allowing them to choose to capture a photo or view the feed
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String PHOTO_LIST = "photo_list";
    public static final String NEW_PHOTO = "new_photo";
    public static final int PHOTO_CAPTURE = 1;
    public static final int PHOTO_PICK = 2;
    public static final int VIEW_FEED = 3;
    Button captureButton;
    Button feedButton;
    ArrayList<String> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // If the device is running Lollipop or better, use a slide in from the top animation when this
        // Activity's layout is inflated. Commented out because the animations overlap, it looks a little sloppy
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
//            getWindow().setEnterTransition(new Slide(Gravity.TOP));
//        }

        // Inflate the layout and create the toolbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("PhotoApp");
        setSupportActionBar(toolbar);

        // Assign the Buttons to their appropriate views
        captureButton = (Button) findViewById(R.id.captureButton);
        feedButton = (Button) findViewById(R.id.feedButton);

        // Initialize the list of image URI's
        photoList = new ArrayList<>();

        // Check for a saved instance state containing the list of image URI's from a previous instantiation
        // of this activity that killed (in the application's current lifecycle). If one exists,
        // populate the current photoList with the old URI's
        if(savedInstanceState != null && savedInstanceState.getStringArrayList(MainActivity.PHOTO_LIST) != null) {
            photoList = savedInstanceState.getStringArrayList(MainActivity.PHOTO_LIST);
            Log.d(TAG, photoList.size() + " photos loaded");
        } else {
            Log.d(TAG, "no savedinstancestate photolist");
        }

        // If the capture button is clicked, capture a photo
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });

        // If the feed button is clicked, start the FeedActivity, passing the current list of image
        // URI's in photoList so that FeedActivity can display them. Use startActivityForIntent in case
        // the user wants to capture an image from the FeedActivity (clicks the capture button from FeedActivity).
        feedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent feedIntent = new Intent(MainActivity.this, FeedActivity.class);
                feedIntent.putStringArrayListExtra(PHOTO_LIST, photoList);
                startActivityForResult(feedIntent, VIEW_FEED);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If the Activity that finished was a photo capture, get the URI of the taken photo and add it
        // to the beginning of the list of image URI's (so its displayed at the top of the FeedActivity's
        // RecyclerView. Then, start the feedActivity with an extra NEW_PHOTO (containing the new photo URI)
        // so that the FeedActivity knows that this newly captured photo will need resizing
        if(resultCode == RESULT_OK && requestCode == PHOTO_CAPTURE) {
            String photoUri = data.getData().toString();
            Log.d(TAG, photoUri);
            photoList.add(0, photoUri);
            Intent feedIntent = new Intent(MainActivity.this, FeedActivity.class);
            feedIntent.putStringArrayListExtra(PHOTO_LIST, photoList);
            feedIntent.putExtra(NEW_PHOTO, photoUri);
            startActivityForResult(feedIntent, VIEW_FEED);
        }

        // If the Activity that finished was an action get content activity
        if(resultCode == RESULT_OK && requestCode == PHOTO_PICK) {
            // Retrieve the Uri of the photo the user picked
            String photoUri = data.getData().toString();
            Log.d(TAG, photoUri + " ");
            String photoUriString = null;

            // Copy the image file chosen by the user to the external storage public directory pictures
            // directory so that when it gets resized in FeedActivity, it will not replace the original image
            try {
                File photoCopyFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), UUID.randomUUID().toString() + ".jpg");
                photoUriString = Uri.fromFile(photoCopyFile).toString();
                InputStream is = MainActivity.this.getContentResolver().openInputStream(Uri.parse(photoUri));
                OutputStream os = MainActivity.this.getContentResolver().openOutputStream(Uri.fromFile(photoCopyFile));

                if(is != null && os != null) {
                    BufferedInputStream inputStream = new BufferedInputStream(is);
                    BufferedOutputStream outputStream = new BufferedOutputStream(os);
                    byte[] buffer = new byte[1024];
                    while (inputStream.read(buffer) != -1) {
                        outputStream.write(buffer);
                    }
                    outputStream.close();
                    inputStream.close();
                }

                is.close();
                os.close();
                Log.d(TAG, "photo copy " + photoCopyFile.getPath());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            // Add the photo Uri to the list of photo URI's
            if(photoUriString != null) {
                photoList.add(0, photoUriString);
            }

            Log.d(TAG, "Photo list " + photoList.size() + " " + photoList.get(0));

            // Start the FeedActivity with an extra NEW_PHOTO (containing the new photo URI)
            // so that the FeedActivity knows that this newly captured photo will need resizing
            Intent feedIntent = new Intent(MainActivity.this, FeedActivity.class);
            feedIntent.putStringArrayListExtra(PHOTO_LIST, photoList);
            feedIntent.putExtra(NEW_PHOTO, photoUriString);
            startActivityForResult(feedIntent, VIEW_FEED);
        }

        // If the Activity that was finished was the FeedActivity and there exists an extra requesting
        // that a photo capture occur, capture a photo
        if(resultCode == RESULT_OK && requestCode == VIEW_FEED) {
            Log.d(TAG, "Feed closed");
            if(data.getBooleanExtra(FeedActivity.CAPTURE_REQUESTED, false)) {
                capturePhoto();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // When this activity is killed, save the StringArray photoList that contains the
        // URI's of the images capture (or picked) in this lifecycle of the application
        outState.putStringArrayList(MainActivity.PHOTO_LIST, photoList);
        Log.d(TAG, "photoList saved");
    }

    /**
     * Check for a Camera on the device and check for camera permissions. If the device
     * has a camera and the app has camera permissions, start an image capture intent.
     * If not, start an action get content intent so the user can pick an image from their files.
     */
    private void capturePhoto() {
        if(MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) && ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
            Intent photoCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(photoCaptureIntent, PHOTO_CAPTURE);
        } else {
            Intent photoPickIntent = new Intent(Intent.ACTION_GET_CONTENT);
            photoPickIntent.setType("image/*");
            startActivityForResult(Intent.createChooser(photoPickIntent, "Select Image"), PHOTO_PICK);
        }
    }
}
