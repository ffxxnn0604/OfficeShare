package edu.usc.officeshare.server;

import java.io.File;

import android.os.Environment;
import android.util.Log;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;

import edu.usc.officeshare.util.Utility;

public class ServerFileTransfer implements Runnable {
	
	private final String TAG = "ServerFileTransfer";
	
	private final SocketUDT clientSocket;
	
	private String filePathUnderSDRoot = "";
	
	public ServerFileTransfer(SocketUDT newClientSocket){
		clientSocket = newClientSocket;
		filePathUnderSDRoot = "BigJava5.pdf"; //the path to a file should be passed through ctor
	}
	
	@Override
	public void run() {
		String filename;
		int fileNameLength;
		byte[] buffer;
		long requestFileSize;
		
		try{
			//1. check if SD card is mounted or not
			if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
				buffer = new byte[8];
				buffer = Utility.longToByte(-1); //send size as -1 if SD card is not mounted
				clientSocket.send(buffer);
				Log.d(TAG, "The SD card is not mounted, send -1 as the size, closing socket.");
				clientSocket.close();
				return;
			}
			
			//2. if SD is present, concatenate the path to sd root and path to file
			File dirToSDRoot = Environment.getExternalStorageDirectory();
			File requestFile = new File(dirToSDRoot, filePathUnderSDRoot);
			
			//3. check if the file exist or not
			if (!requestFile.exists()){
				// if file doesn't exist, send 0 as the file size
				// then close the socket, as this is a incorrect request
				buffer = new byte[8];
				buffer = Utility.longToByte(0);
				clientSocket.send(buffer);
				Log.d(TAG, "File doesn't exist, send 0 as the size, closing socket.");
				clientSocket.close();
				return;
			}
			
			//4. file exist, let's extract the filename and its length
			filename = requestFile.getName();
			Log.d(TAG, "The filename is " + filename);
			fileNameLength = filename.length();
			Log.d(TAG, "The filename's length is " + fileNameLength);
			
			//5. send the length of filename
			buffer = new byte[4];
			buffer = Utility.intToByte(fileNameLength);
			clientSocket.send(buffer);
			Log.d(TAG, "sent length of filename "+ fileNameLength);
			
			//6. send the filename
			buffer = new byte[fileNameLength];
			buffer = filename.getBytes();
			clientSocket.send(buffer);
			Log.d(TAG, "sent the filename " + filename);
			
			//7. send the size of the file
			requestFileSize = requestFile.length();
			buffer = new byte[8];
			buffer = Utility.longToByte(requestFileSize);
			clientSocket.send(buffer);
			Log.d(TAG, "Send the file size " + requestFileSize);
			
			//8. send the file
			clientSocket.sendFile(requestFile, 0, requestFileSize);
			Log.d(TAG, "File sent.");
			
			//9. finish sending file, close client socket (accept socket is open)
			clientSocket.close();
			Log.d(TAG, "Socket closed.");			
			
		}
		catch (ExceptionUDT u){
			u.printStackTrace();
		}
		
/*		try {
			
			Log.d(TAG, "Inside a client handle thread.");
			
			//1. get length of file name
			buffer = new byte[4];
			clientSocket.receive(buffer);
			fileNameLength = Utility.byteToInt(buffer);
			Log.d(TAG, "The file length is " + fileNameLength);
			
			//2. get fileName
			buffer = new byte[fileNameLength];
			clientSocket.receive(buffer);
			filename = new String(buffer);
			Log.d(TAG, "The file name is " + filename + ", its length is " + filename.length());
			
			//3. check if the SD card is mounted or not
			if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
				buffer = new byte[8];
				buffer = Utility.longToByte(-1); //send size as -1 if SD card is not mounted
				clientSocket.send(buffer);
				Log.d(TAG, "The SD card is not mounted, send -1 as the size, closing socket.");
				clientSocket.close();
				return;
			}
			
			//4. check if the file exist, and get handle on it
			File dirToSDRoot = Environment.getExternalStorageDirectory();
			File requestFile = new File(dirToSDRoot, filename);//"request.txt");
			Log.d(TAG, "The file path is " + requestFile.getAbsolutePath());
			if (!requestFile.exists()){
				// if file doesn't exist, send 0 as the file size
				// then close the socket, as this is a incorrect request
				buffer = new byte[8];
				buffer = Utility.longToByte(0);
				clientSocket.send(buffer);
				Log.d(TAG, "File doesn't exist, send 0 as the size, closing socket.");
				clientSocket.close();
				return;
			}
			
			Log.d(TAG, "The file exist!");
			
			//5. send file size information
			requestFileSize = requestFile.length();
			buffer = new byte[8];
			buffer = Utility.longToByte(requestFileSize);
			clientSocket.send(buffer);
			Log.d(TAG, "Send the file size " + requestFileSize);
			
			//6. send the file
			clientSocket.sendFile(requestFile, 0, requestFileSize);
			Log.d(TAG, "File sent.");
			
			//7. finish sending file, close client socket (accept socket is open)
			clientSocket.close();
			Log.d(TAG, "Socket closed.");					
			
		} catch (ExceptionUDT e) {
			e.printStackTrace();
		}*/
		
	}	
	
}