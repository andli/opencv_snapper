package com.example.an_ehs.opencv_snapper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener  {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_VIDEO_CAPTURE = 1;

    private ImageView mImageView;
    private Bitmap imageBitmap;
    private SeekBar seekBar;

    private static final String TAG = "MainActivity";

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mImageView = (ImageView) findViewById(R.id.mImageView);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        setSupportActionBar(toolbar);
        seekBar.setEnabled(false);
        initViews();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }*/
                mImageView.setImageResource(R.mipmap.testboard01);
                if (!seekBar.isEnabled()) seekBar.setEnabled(true);
            }
        });
    }

    private void initViews(){
        mImageView = (ImageView)findViewById(R.id.mImageView);
        imageBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_crop_original_black_24dp);
        mImageView.setImageBitmap(imageBitmap);
        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
            if (!seekBar.isEnabled()) seekBar.setEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    private Bitmap manipulateBitmap(Bitmap bitmap, int value){

        Log.d(TAG, " Filtering ... ");
        Mat src = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_gray = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat dst = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap,src,true);
        //src.convertTo(src,-1,1,value);
        
        // Convert to grayscale
        Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_RGB2GRAY);

        // Improve contrast?
        Imgproc.equalizeHist(src_gray,src_gray);

        //blur image
        Size s = new Size(5,5);
        Imgproc.GaussianBlur(src_gray, src_gray, s, 0);

        /*//apply adaptive treshold
        Imgproc.adaptiveThreshold( src_gray, src_gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,value,2);

        //adding secondary treshold, removes a lot of noise
        Imgproc.threshold(src_gray, dst, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);*/
        
        Imgproc.adaptiveThreshold(src_gray, dst, 75, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 7,  value);

        Bitmap result = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,result);
        return result;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Bitmap edited = manipulateBitmap(imageBitmap, progress);
        mImageView.setImageBitmap(edited);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
