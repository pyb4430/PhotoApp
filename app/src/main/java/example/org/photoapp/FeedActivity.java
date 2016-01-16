package example.org.photoapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Displays the images captured in this lifecycle of the app in reverse chronological order
 */
public class FeedActivity extends AppCompatActivity {

    public final static String TAG = FeedActivity.class.getSimpleName();
    public final static String CAPTURE_REQUESTED = "capture_requested";
    RecyclerView mRecyclerView;
    ArrayList<String> photoList;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private Button captureButton;
    private Button feedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Photo Feed");
        setSupportActionBar(toolbar);

        // Retrieve the list of photo URI's passed from the MainActivity
        Intent intent = getIntent();
        photoList = intent.getStringArrayListExtra(MainActivity.PHOTO_LIST);

        // Assign the capture and feed buttons to their views
        captureButton = (Button) findViewById(R.id.captureButtonFeed);
        feedButton = (Button) findViewById(R.id.feedButtonFeed);

        // If this activity was started because a new photo has been taken or selected, retrieve the
        // Uri of the new photo and begin a ResizeImageAsync task to resize the image if it is not
        // already the target width specified in the ResizeImageAsync class. Checking the size is necessary
        // because an orientation change will cause a "new photo" that has actually already been resized
        // to get through.
        if(intent.hasExtra(MainActivity.NEW_PHOTO)) {
            String newPhotoUri = intent.getStringExtra(MainActivity.NEW_PHOTO);

            // Create an options variable that will return the size of an image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            // Get the size of the new image.
            try {
                InputStream inputStream = FeedActivity.this.getContentResolver().openInputStream(Uri.parse(newPhotoUri));

                if(inputStream != null) {
                    BitmapFactory.decodeStream(inputStream, null, options);
                }

                inputStream.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            int width = options.outWidth;

            if(width != ResizeImageAsync.TARGET_WIDTH) {
                ResizeImageAsync resizeImageAsync = new ResizeImageAsync(FeedActivity.this, newPhotoUri);
                resizeImageAsync.execute();
            }
        }

        // Assign the RecyclerView to its view, create and assign a new layout manager to the RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.feedRecyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Create a new adapter with the list of photo URI's and set it as the RecyclerView's adapter
        mAdapter = new PhotoAdapter(photoList, FeedActivity.this);
        mRecyclerView.setAdapter(mAdapter);

        // If the capture button is clicked, close this FeedActivity and return to the MainActivity
        // with the intention of capturing a photo, expressed via a boolean extra placed in the intent
        // to be received by the onActivityResult method of the MainActivity
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                intent.putExtra(CAPTURE_REQUESTED, true);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        // If the feed button is clicked, return to the top of the feed
        feedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutManager.scrollToPosition(0);
            }
        });
    }

    public class ResizeImageAsync extends AsyncTask<Void, Void, Void> {

        public final static int TARGET_WIDTH = 1080;
        private String photoUriString;
        private Context mContext;

        /**
         * Constructor for the ResizeImageAsync class,
         * @param context Context of the calling class
         * @param photoUriString The Uri for the image that needs resizing
         */
        public ResizeImageAsync(Context context, String photoUriString) {
            super();
            this.photoUriString = photoUriString;
            mContext = context;

        }

        @Override
        protected Void doInBackground(Void... params) {
            // Create an options variable that will return the size of an image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            // Get the size of the image that needs resizing
            try {
                InputStream inputStream = mContext.getContentResolver().openInputStream(Uri.parse(photoUriString));

                if(inputStream != null) {
                    BitmapFactory.decodeStream(inputStream, null, options);
                }

                inputStream.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            int height = options.outHeight;
            int width = options.outWidth;


            Bitmap tempBitmap;
            try {
                // Open in input stream to the image
                InputStream inputStream = mContext.getContentResolver().openInputStream(Uri.parse(photoUriString));

                Log.d(TAG, photoUriString);
                Log.d(TAG, "width and height of image: " + width + " " + height);

                // Calculate the the smallest factor of 2 that can be used as the inSampleSize value in
                // order to decode the image to a bitmap that is as small as possible without being smaller than
                // the ultimate target image width
                int inSampleSize = 1;
                if(width > TARGET_WIDTH) {
                    while (((width / 2) / inSampleSize) > TARGET_WIDTH) {
                        inSampleSize *= 2;
                    }
                }

                // Decode the image to a bitmap and close the input stream
                options.inJustDecodeBounds = false;
                options.inSampleSize = inSampleSize;
                if(inputStream != null) {
                    tempBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    Log.d(TAG, tempBitmap.getWidth() + " " + tempBitmap.getHeight() + " " + inSampleSize);
                    inputStream.close();

                    // Calculate the necessary ultimate target image height necessary to preserve the image's
                    // aspect ratio based on the image's initial height and width
                    float aspectRatio = ((float) height) / ((float) width);

                    // Scale the bitmap to the target height and width
                    tempBitmap = Bitmap.createScaledBitmap(tempBitmap, TARGET_WIDTH, (int) (TARGET_WIDTH * aspectRatio), false);

                    Log.d(TAG, tempBitmap.getWidth() + " " + tempBitmap.getHeight());

                    // Write the new scaled bitmap to the old image file
                    try {
                        OutputStream outputStream = mContext.getContentResolver().openOutputStream(Uri.parse(photoUriString));
                        tempBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mAdapter.notifyDataSetChanged();
        }
    }

}
