package edu.usc.officeshare.signal;

import org.alljoyn.bus.annotation.Position;

import edu.usc.officeshare.util.Utility;

public class Point {
	
	//TODO convert this to enum, right now type could be assigned any int value
	public static final int START = 0;
	public static final int NORMAL = 1;
	
	@Position(0)
	public int type;
	@Position(1)
	public double x;
	@Position(2)
	public double y;
	
	public Point(){
		type = NORMAL;
		x = 0.0;
		y = 0.0;
	}
	
	public Point(Point mP){
		type = mP.type;
		x = mP.x;
		y = mP.y;
	}
	
	public Point(int mPT, double mX, double mY){
		type = mPT;
		x = mX;
		y = mY;
	}
	
	public static byte[] toByteArray(Point mP){
		byte[] result = new byte[20]; //double is 8, int is 4, 8*2+4 = 20
		byte[] bType = Utility.toByteArray(mP.type);
		byte[] bX = Utility.toByteArray(mP.x);
		byte[] bY = Utility.toByteArray(mP.y);
		System.arraycopy(bType, 0, result, 0, bType.length);
		System.arraycopy(bX, 0, result, bType.length, bX.length);
		System.arraycopy(bY, 0, result, bType.length + bX.length, bY.length);
		return result;		
	}
	
	public static Point toPoint(byte[] bP) {
		Point result = new Point();
		byte[] bType = new byte[4];
		byte[] bX = new byte[8];
		byte[] bY = new byte[8];
		System.arraycopy(bP, 0, bType, 0, bType.length);
		System.arraycopy(bP, bType.length, bX, 0, bX.length);
		System.arraycopy(bP, bType.length + bX.length, bY, 0, bY.length);
		result.type = Utility.toInt(bType);
		result.x = Utility.toDouble(bX);
		result.y = Utility.toDouble(bY);		
		return result; 
	}
}
