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
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import static org.opencv.imgproc.Imgproc.putText;
import static org.opencv.imgproc.Imgproc.warpPerspective;

/**
 * Created by an-ehs on 2016-12-05.
 */

public class Ricochet {

    private static final String TAG = "Ricochet";
    private Point CENTER;

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
        CENTER = new Point(bitmap.getWidth() / 2, bitmap.getHeight() / 2);

        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_hsv = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_thresh = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat src_cnt = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat dst = null;
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
        HoughLines(src_cnt, lines, 2, 1.7 * Math.PI / 180, bitmap.getWidth() / 2);//, bitmap.getWidth() / 16, bitmap.getWidth() / 40);

        cvtColor(src_cnt, src_cnt, COLOR_GRAY2BGR);
        /*for(int i = 0; i < lines.rows(); i++) {
            double[] val = lines.get(i, 0);
            Log.d(TAG, "line: " + val.length);
            line(src_cnt, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(255, 0, 255), 3);
        }*/
        Scalar colorPurple = new Scalar(255, 0, 255);
        Scalar colorGreen = new Scalar(0, 255, 0);
        Scalar colorYellow = new Scalar(255, 255, 0);

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

            allLines.add(new Point[]{new Point(x0 - b, y0 + a), new Point(x0 + b, y0 - a)});
        }
        Log.d(TAG, "all lines: " + allLines.size());

        int ptNum = 1;
        // Draw the intersection points
        for (int i = 0; i < allLines.size(); i++) {
            Point[] outerLine = allLines.get(i);

            for (int j = i + 1; j < allLines.size(); j++) {
                Point[] innerLine = allLines.get(j);

                Point pt = getIntersectionPoint(outerLine[0], outerLine[1], innerLine[0], innerLine[1]);
                if (pt != null) {
                    //Log.d(TAG, "first line pt1: " + outerLine[0].x + ", " + outerLine[0].y + "  first line pt2: " + outerLine[1].x + ", " + outerLine[1].y);
                    //Log.d(TAG, "second line pt1: " + innerLine[0].x + ", " + innerLine[0].y + "  second line pt2: " + innerLine[1].x + ", " + innerLine[1].y);
                    boolean added = addPointFiltered(intersectPoints, pt);

                    if (added) {
                        circle(src_cnt, pt, 16, colorGreen, 5);
                        //putText(src_cnt, String.valueOf(ptNum), new Point(pt.x - 30, pt.y + 80), Core.FONT_HERSHEY_PLAIN, 4, colorGreen, 5);
                        ptNum = ptNum + 1;
                    }
                }
            }
        }
        Log.d(TAG, "intersects points: " + intersectPoints.size());

        Bitmap result;
        if (intersectPoints.size() == 4) {
            // Reorder points
            ArrayList<Point> sortedPoints = orderRectCorners(intersectPoints);
            ptNum = 1;
            for (int i = 0; i < sortedPoints.size(); i++) {
                putText(src_cnt, String.valueOf(ptNum), new Point(sortedPoints.get(i).x - 30, sortedPoints.get(i).y + 80), Core.FONT_HERSHEY_PLAIN, 4, colorYellow, 5);
                ptNum++;
            }
            // Warp the image
            result = warp(bitmap, sortedPoints.get(0), sortedPoints.get(1), sortedPoints.get(2), sortedPoints.get(3));
            return result;

        }
        else {
            throw new NullPointerException();
        }
        // Return
        /*dst = src_cnt.clone();
        result = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, result);
        return result;*/
    }

    // top-left = 0; top-right = 1;
    // right-bottom = 2; left-bottom = 3;
    private ArrayList<Point> orderRectCorners(ArrayList<Point> corners) {
        if (corners.size() == 4) {
            ArrayList<Point> ordCorners = orderPointsByRows(corners);

            if (ordCorners.get(0).x > ordCorners.get(1).x) { // swap points
                Point tmp = ordCorners.get(0);
                ordCorners.set(0, ordCorners.get(1));
                ordCorners.set(1, tmp);
            }

            if (ordCorners.get(2).x < ordCorners.get(3).x) { // swap points
                Point tmp = ordCorners.get(2);
                ordCorners.set(2, ordCorners.get(3));
                ordCorners.set(3, tmp);
            }
            return ordCorners;
        }
        return null;
    }

    private ArrayList<Point> orderPointsByRows(ArrayList<Point> points) {
        Collections.sort(points, new Comparator<Point>() {
            public int compare(Point p1, Point p2) {
                if (p1.y < p2.y) return -1;
                if (p1.y > p2.y) return 1;
                return 0;
            }
        });
        return points;
    }

    private boolean addPointFiltered(ArrayList<Point> list, Point newPoint) {
        final int NEAR_THRESHOLD = 10;
        boolean tooClose = false;
        if (newPoint.x < 0 || newPoint.y < 0) {
            return false;
        }
        for (Point pt : list) {
            if (Math.abs(pt.x - newPoint.x) < NEAR_THRESHOLD && Math.abs(pt.y - newPoint.y) < NEAR_THRESHOLD) {
                //Log.d(TAG, "skipped " + Math.abs(pt.x - newPoint.x));
                //Log.d(TAG, "skipped " + Math.abs(pt.y - newPoint.y));
                return false;
            }
        }

        //Log.d(TAG, "added " + newPoint.toString());
        list.add(newPoint);
        return true;
    }

    private Point getIntersectionPoint(Point point1, Point point2, Point point3, Point point4) {
        final double EPSILON = 0.00001;

        //Log.d(TAG, "Checking..." + point1 + "," + point2 + "," + point3 + "," + point4);

        double a1 = (point1.y - point2.y) / (double) (point1.x - point2.x);
        double b1 = point1.y - a1 * point1.x;

        double a2 = (point3.y - point4.y) / (double) (point3.x - point4.x);
        double b2 = point3.y - a2 * point3.x;

        if (Math.abs(a1 - a2) < EPSILON)
            return null;

        double x = (b2 - b1) / (a1 - a2);
        double y = a1 * x + b1;
        return new Point(x, y);
    }

    private Bitmap warp(Bitmap image, Point topLeft, Point topRight, Point bottomRight, Point bottomLeft) {
        int resultWidth = (int) (topRight.x - topLeft.x);
        int bottomWidth = (int) (bottomRight.x - bottomLeft.x);
        if (bottomWidth > resultWidth)
            resultWidth = bottomWidth;

        int resultHeight = (int) (bottomLeft.y - topLeft.y);
        int bottomHeight = (int) (bottomRight.y - topRight.y);
        if (bottomHeight > resultHeight)
            resultHeight = bottomHeight;

        Mat inputMat = new Mat(image.getHeight(), image.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(image, inputMat);
        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC1);

        List<Point> source = new ArrayList<>();
        source.add(topLeft);
        source.add(topRight);
        source.add(bottomLeft);
        source.add(bottomRight);
        Mat startM = Converters.vector_Point2f_to_Mat(source);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(resultWidth, 0);
        Point ocvPOut3 = new Point(0, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, resultHeight);
        List<Point> dest = new ArrayList<>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = getPerspectiveTransform(startM, endM);

        warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight));

        Bitmap output = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputMat, output);
        return output;
    }

    private static int ValMax(int value, int max) {
        return value * max / 100;
    }
}
