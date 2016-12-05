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

    public Bitmap manipulateBitmap(Bitmap bitmap, int value1, int value2, int value3){

        int val1 = ValMax(value1, 255);
        int val2 = ValMax(value2, 13);
        int val3 = ValMax(value3, 255);
        Log.d(TAG, " Filtering with values " + val1 + ", " + val2 + ", " + val3);
        Mat src = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_gray = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv_low = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv_up = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_thresh = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat dst = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap,src,true);
        //src.convertTo(src,-1,1,value);

        Imgproc.cvtColor(src, src_hsv, Imgproc.COLOR_BGR2HSV);
        Core.inRange(src_hsv, new Scalar(107, 0, 0), new Scalar(255, 76, 255),  src_thresh);

        // Improve contrast?
        Imgproc.equalizeHist(src_thresh,src_thresh);

        //blur image
        int sizeElement = 5;
        Size s = new Size(sizeElement,sizeElement);
        Imgproc.GaussianBlur(src_thresh, src_thresh, s, 0);

        for(int i = 0; i<6; i++) {
            Imgproc.dilate(src_thresh, src_thresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, s));
        }
        for(int i = 0; i<6; i++) {
            Imgproc.erode(src_thresh, src_thresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, s));
        }

        Imgproc.adaptiveThreshold(src_thresh, src_thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 7,  4);
        Core.bitwise_not(src_thresh,src_thresh);



        // Find the contours of the board
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(src_thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxVal = 0;
        int maxValIdx = 1;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        Imgproc.cvtColor(src_thresh, dst, Imgproc.COLOR_GRAY2BGR);
        Imgproc.drawContours(dst, contours, maxValIdx, new Scalar(0,255,0), 5);
/*
        //Imgproc.drawContours(dst, contours, val, new Scalar(255, 255, 255), 2);

          /* 1 rect = cv2.minAreaRect(cnt)
    2 box = cv2.boxPoints(rect)
    3 box = np.int0(box)
    4 cv2.drawContours(img,[box],0,(0,0,255),2)

        //dst = src_thresh.clone();*/

        // Convert to color
        Bitmap result = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,result);
        return result;
    }

    private static int ValMax(int value, int max) {
        return value * max / 100;
    }
}
