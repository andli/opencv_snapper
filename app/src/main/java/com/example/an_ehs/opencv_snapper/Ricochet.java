package com.example.an_ehs.opencv_snapper;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.HoughLines;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.equalizeHist;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.warpPerspective;

/**
 * Created by an-ehs on 2016-12-05.
 */

public class Ricochet {

    private static final String TAG = "Ricochet";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    public Ricochet() {
    }

    public Bitmap manipulateBitmap(Bitmap bitmap, int value1, int value2, int value3) {

        int val1 = ValMax(value1, 10);
        int val2 = ValMax(value2, 32);
        int val3 = ValMax(value3, 255);
        Log.d(TAG, " Filtering with values " + val1 + ", " + val2 + ", " + val3);
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_thresh = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_cnt = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat dst = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, src, true);
        //src.convertTo(src,-1,1,value);

        cvtColor(src, src_hsv, COLOR_BGR2HSV);
        Core.inRange(src_hsv, new Scalar(107, 0, 0), new Scalar(255, 76, 255), src_thresh);

        // Improve contrast?
        equalizeHist(src_thresh, src_thresh);

        //blur image
        int sizeElement = 3;
        Size s = new Size(sizeElement, sizeElement);
        GaussianBlur(src_thresh, src_thresh, s, 0);

        for (int i = 0; i < 8; i++) {
            dilate(src_thresh, src_thresh, getStructuringElement(MORPH_RECT, s));
        }
        for (int i = 0; i < 8; i++) {
            erode(src_thresh, src_thresh, getStructuringElement(MORPH_RECT, s));
        }

        adaptiveThreshold(src_thresh, src_thresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 7, 4);
        Core.bitwise_not(src_thresh, src_thresh);

        // Find the contours of the board
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        findContours(src_thresh, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        double maxVal = 0;
        int maxValIdx = 1;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            double contourArea = contourArea(contours.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        MatOfPoint largestContour = contours.get(maxValIdx);

        cvtColor(src_cnt, src_cnt, COLOR_GRAY2BGR);
        drawContours(src_cnt, contours, maxValIdx, new Scalar(255, 255, 255), 3);
        cvtColor(src_cnt, src_cnt, COLOR_BGR2GRAY);

        Mat lines = new Mat();
        // 1, 2, rho, theta,                        threshold, minLineLength, maxLineGap
        if (val1 == 0) val1 = 1;
        Log.d(TAG, "bitmap width: " + bitmap.getWidth());
        HoughLines(src_cnt, lines, 2, Math.PI / 180, bitmap.getWidth() / 2);//, bitmap.getWidth() / 16, bitmap.getWidth() / 40);

        cvtColor(src_cnt, src_cnt, COLOR_GRAY2BGR);
        /*for(int i = 0; i < lines.rows(); i++) {
            double[] val = lines.get(i, 0);
            Log.d(TAG, "line: " + val.length);
            line(src_cnt, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(255, 0, 255), 3);
        }*/
        Scalar colorPurple = new Scalar(255, 0, 255);
        Scalar colorGreen = new Scalar(0, 255, 0);

        ArrayList<Point[]> allLines = new ArrayList<>();
        ArrayList<Point> intersectPoints = new ArrayList<>();

        // Draw the lines
        double[] data;
        double rho, theta;
        Point pt1 = new Point();
        Point pt2 = new Point();
        double a, b;
        double x0, y0;
        int scaleFactor = bitmap.getWidth() * 2;
        for (int i = 0; i < lines.rows(); i++) {
            data = lines.get(i, 0);
            rho = data[0];
            theta = data[1];
            a = Math.cos(theta);
            b = Math.sin(theta);
            x0 = a * rho;
            y0 = b * rho;
            pt1.x = Math.round(x0 + scaleFactor * (-b));
            pt1.y = Math.round(y0 + scaleFactor * a);
            pt2.x = Math.round(x0 - scaleFactor * (-b));
            pt2.y = Math.round(y0 - scaleFactor * a);
            line(src_cnt, pt1, pt2, colorPurple, 5);

            //allLines.add(new Point[]{ new Point(x0 - b, y0 + a), new Point(x0 + b, y0 - a) });
            allLines.add(new Point[]{pt1, pt2});
        }
        allLines.add(new Point[]{new Point(-100, -100), new Point(200, 200)});
        allLines.add(new Point[]{new Point(100, 200), new Point(400, 100)});

        Log.d(TAG, "all lines: " + allLines.size());

        // Draw the intersection points
        for (int i = 0; i < allLines.size(); i++) {
            Point[] outerLine = allLines.get(i);
            Log.d(TAG, "first line pt1: " + outerLine[0].x + ", " + outerLine[0].y + "  first line pt2: " + outerLine[1].x + ", " + outerLine[1].y);

            for (int j = i + 1; j < allLines.size(); j++) {
                Point[] innerLine = allLines.get(j);
                Log.d(TAG, "second line pt1: " + innerLine[0].x + ", " + innerLine[0].y + "  second line pt2: " + innerLine[1].x + ", " + innerLine[1].y);
                boolean intersects = intersects(outerLine[0], outerLine[1], innerLine[0], innerLine[1]);

                if (intersects) {
                    Point pt = getIntersectionPoint(outerLine[0], outerLine[1], innerLine[0], innerLine[1]);
                    intersectPoints.add(pt);
                    circle(src_cnt, pt, 16, colorGreen, 3);
                }
            }
        }
        Log.d(TAG, "intersects points: " + intersectPoints.size());

        // Return
        dst = src_cnt.clone();
        Bitmap result = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, result);
        return result;
    }

    private Point getIntersectionPoint(Point point1, Point point2, Point point3, Point point4) {
        final double EPSILON = 0.00001;

        double a1 = (point1.x - point2.y) / (double) (point1.x - point2.x);
        double b1 = point1.y - a1 * point1.x;

        double a2 = (point3.y - point4.y) / (double) (point3.x - point4.x);
        double b2 = point3.y - a2 * point3.x;

        if (Math.abs(a1 - a2) < EPSILON)
            throw new ArithmeticException();

        double x = (b2 - b1) / (a1 - a2);
        double y = a1 * x + b1;
        return new Point(x, y);
    }

    public static int orientation(Point p, Point q, Point r) {
        double val = (q.y - p.y) * (r.x - q.x)
                - (q.x - p.x) * (r.y - q.y);

        if (val == 0.0)
            return 0; // colinear
        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    public static boolean intersects(Point p1, Point q1, Point p2, Point q2) {

        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        Log.d(TAG, "..." + p1 + "," + q1 + "," + p2 + "," + q2);
        Log.d(TAG, "..." + o1 + "," + o2 + "," + o3 + "," + o4);

        if (o1 != o2 && o3 != o4)
            return true;

        return false;
    }

    public Mat warp(Mat inputMat, Mat startM) {
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
