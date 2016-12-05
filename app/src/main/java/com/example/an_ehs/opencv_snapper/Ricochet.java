package com.example.an_ehs.opencv_snapper;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by an-ehs on 2016-12-05.
 */

public class Ricochet {

    private static final String TAG = "Ricochet";

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    public Ricochet() {}

    public Bitmap manipulateBitmap(Bitmap bitmap, int value){

        int val = ValMax(value, 255);
        Log.d(TAG, " Filtering with value " + val);
        Mat src = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_gray = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv_low = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv_up = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_thresh = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat dst = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap,src,true);
        //src.convertTo(src,-1,1,value);

        Imgproc.cvtColor(src, src_hsv, Imgproc.COLOR_RGB2HSV);
        Core.inRange(src_hsv, new Scalar(331, 18, 89), new Scalar(333, 208, 186),  src_hsv);
        //Core.inRange(src_hsv, new Scalar(25, 23, 74), new Scalar(27, 31, 62), src_hsv);
        dst = src_hsv.clone();

        // Convert to grayscale
        List<Mat> hsv_channel = new ArrayList<Mat>();
        Core.split( src_hsv, hsv_channel );
        try
        {
            hsv_channel.get(0).setTo(new Scalar(145));

            Log.v(TAG, "Got the Channel!");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.v(TAG, "Didn't get any channel");
        }
        //dst = hsv_channel.get(0).setTo(new Scalar(145));

/*

        Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_RGB2GRAY);
        // Improve contrast?
        Imgproc.equalizeHist(src_gray,src_gray);

        //blur image
        Size s = new Size(5,5);
        Imgproc.GaussianBlur(src_gray, src_gray, s, 0);
/*
        Imgproc.adaptiveThreshold( src_gray, src_gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,value,2);

        //adding secondary treshold, removes a lot of noise
        Imgproc.threshold(src_gray, dst, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);*//*
*/
/*

        Imgproc.adaptiveThreshold(src_gray, src_thresh, 245, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 7,  4);

        Imgproc.erode(src_thresh, src_thresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13,13)));
        Imgproc.dilate(src_thresh, src_thresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13,13)));

        // Find the contours of the board
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(src_thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        //Imgproc.drawContours(dst, contours, -1, new Scalar(255, 255, 255), 1);
        Imgproc.drawContours(dst, contours, val, new Scalar(255, 255, 255), 2);

           1 rect = cv2.minAreaRect(cnt)
    2 box = cv2.boxPoints(rect)
    3 box = np.int0(box)
    4 cv2.drawContours(img,[box],0,(0,0,255),2)

        //dst = src_thresh.clone();*/

        Bitmap result = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,result);
        return result;
    }

    private static int ValMax(int value, int max) {
        return value * max / 100;
    }
}
