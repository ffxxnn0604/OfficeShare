package edu.usc.officeshare.util;

import java.util.ArrayList;

import edu.usc.officeshare.signal.Point;

public class DrawingConversion {
	
	public static byte[] drawingToBytes (ArrayList<Point> newDrawing){
		int mNumPoints = newDrawing.size();
		byte[] result = new byte[20*mNumPoints];
		byte[] mPoint = new byte[20];
		int offset = 0;
		for(Point mP : newDrawing){
			mPoint = Point.toByteArray(mP);
			System.arraycopy(mPoint, 0, result, offset, mPoint.length);
			offset += mPoint.length;
		}	
		
		return result;
	}
	
	public static ArrayList<Point> toDrawing (byte[] bDrawing)
	{
		ArrayList<Point> mDrawing = new ArrayList<Point>();
		byte[] bPoint = new byte[20];
		Point mPoint = null;
		int mNumPoints = bDrawing.length / 20;
		for(int i = 0; i < mNumPoints; i++){
			System.arraycopy(bDrawing, i*20, bPoint, 0, bPoint.length);
			mPoint = Point.toPoint(bPoint);
			mDrawing.add(mPoint);
		}	
		
		return mDrawing;
	}
	
}
