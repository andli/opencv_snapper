package com.example.an_ehs.opencv_snapper;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.opencv.imgproc.Imgproc.*;

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

        int val1 = ValMax(value1, 10);
        int val2 = ValMax(value2, 32);
        int val3 = ValMax(value3, 255);
        Log.d(TAG, " Filtering with values " + val1 + ", " + val2 + ", " + val3);
        Mat src = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_thresh = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_cnt = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Mat dst = new Mat(bitmap.getHeight(),bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap,src,true);
        //src.convertTo(src,-1,1,value);

        cvtColor(src, src_hsv, COLOR_BGR2HSV);
        Core.inRange(src_hsv, new Scalar(107, 0, 0), new Scalar(255, 76, 255),  src_thresh);

        // Improve contrast?
        equalizeHist(src_thresh,src_thresh);

        //blur image
        int sizeElement = 3;
        Size s = new Size(sizeElement,sizeElement);
        GaussianBlur(src_thresh, src_thresh, s, 0);

        for(int i = 0; i<8; i++) {
            dilate(src_thresh, src_thresh, getStructuringElement(MORPH_RECT, s));
        }
        for(int i = 0; i<8; i++) {
            erode(src_thresh, src_thresh, getStructuringElement(MORPH_RECT, s));
        }

        adaptiveThreshold(src_thresh, src_thresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 7,  4);
        Core.bitwise_not(src_thresh,src_thresh);

        // Find the contours of the board
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        findContours(src_thresh, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        double maxVal = 0;
        int maxValIdx = 1;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        MatOfPoint largestContour = contours.get(maxValIdx);

        cvtColor(src_cnt, src_cnt, COLOR_GRAY2BGR);
        drawContours(src_cnt, contours, maxValIdx, new Scalar(255,255,255), 3);
        cvtColor(src_cnt, src_cnt, COLOR_BGR2GRAY);

        Mat lines = new Mat();
        // 1, 2, rho, theta,                        threshold, minLineLength, maxLineGap
        if (val1 == 0) val1 = 1;
        Log.d(TAG, "bitmap width: " + bitmap.getWidth());
        HoughLines(src_cnt, lines, 2, Math.PI / 180, bitmap.getWidth() / 2);//, bitmap.getWidth() / 16, bitmap.getWidth() / 40);

        Log.d(TAG, "lines: " + lines.rows());

        cvtColor(src_cnt, src_cnt, COLOR_GRAY2BGR);
        /*for(int i = 0; i < lines.rows(); i++) {
            double[] val = lines.get(i, 0);
            Log.d(TAG, "line: " + val.length);
            line(src_cnt, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(255, 0, 255), 3);
        }*/
        Scalar color = new Scalar(255, 0, 255);

        double[] data;
        double rho, theta;
        Point pt1 = new Point();
        Point pt2 = new Point();
        double a, b;
        double x0, y0;
        for (int i = 0; i < lines.rows(); i++)
        {
            data = lines.get(i, 0);
            rho = data[0];
            theta = data[1];
            a = Math.cos(theta);
            b = Math.sin(theta);
            x0 = a*rho;
            y0 = b*rho;
            pt1.x = Math.round(x0 + 2000*(-b));
            pt1.y = Math.round(y0 + 2000*a);
            pt2.x = Math.round(x0 - 2000*(-b));
            pt2.y = Math.round(y0 - 2000 *a);
            line(src_cnt, pt1, pt2, color, 3);
        }



        dst = src_cnt.clone();

        Bitmap result = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,result);
        return result;
    }

    private void drawMinAreaRect(Mat src_thresh, Mat dst, MatOfPoint largestContour) {
        MatOfPoint2f cnt2f = new MatOfPoint2f();
        largestContour.convertTo(largestContour, CvType.CV_32F);

        RotatedRect rect = minAreaRect(largestContour);
        Log.d(TAG, rect.toString());

        cvtColor(src_thresh, dst, COLOR_GRAY2BGR);
        Point[] points = new Point[4];
        rect.points(points);
        MatOfPoint points2 = new MatOfPoint(points);
        drawLines(dst, points2, true, new Scalar(255,0,0));
    }

    static void drawLines(Mat img, MatOfPoint points, boolean isClosed, Scalar color) {
        List<MatOfPoint> pointsArray = new ArrayList<MatOfPoint>();
        pointsArray.add(points);
        polylines(img, pointsArray, isClosed, color);
    }

    public Mat warp(Mat inputMat,Mat startM) {
        int resultWidth = 1000;
        int resultHeight = 1000;

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(0, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, 0);
        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = getPerspectiveTransform(startM, endM);

        warpPerspective(inputMat,
                outputMat,
                perspectiveTransform,
                new Size(resultWidth, resultHeight),
                INTER_CUBIC);

        return outputMat;
    }

    private static int ValMax(int value, int max) {
        return value * max / 100;
    }
}
