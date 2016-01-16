package example.org.photoapp;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Taylor on 1/14/2016.
 * A RecyclerViewAdapter to populate the RecyclerView with images given an array of image URI's
 */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    public static final String TAG = PhotoAdapter.class.getSimpleName();
    private ArrayList<String> photoList;
    private Context mContext;

    /**
     * Constructor of PhotoAdapter
     * @param photoList list of image URI's
     * @param context context of calling class
     */
    public PhotoAdapter(ArrayList<String> photoList, Context context) {
        this.photoList = photoList;
        mContext = context;
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the child view's layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_child, parent, false);
        // Create a new view holder to hold the inflated view
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        // Get the size of the image at the given URI
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(Uri.parse(photoList.get(position)));

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

        // If the image at the given URI is not the target width, display the placeholder image.
        // If the image is the target width, set the ImageView Uri to the image Uri
        if(width != FeedActivity.ResizeImageAsync.TARGET_WIDTH) {
            holder.mImageView.setImageResource(R.drawable.placeholder);
        } else {
            holder.mImageView.setImageURI(Uri.parse(photoList.get(position)));
        }

    }

    /**
     * ViewHolder class to provide the Adapter and LayoutManager access to the child view's ImageView
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.photoChildImageView);
        }
    }
}
