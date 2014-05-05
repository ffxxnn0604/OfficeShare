package edu.usc.officeshare.server;

import java.io.File;

import org.alljoyn.bus.sample.chat.ChatApplication;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;

import edu.usc.officeshare.util.FileUtility;
import edu.usc.officeshare.util.Utility;

public class ServerFileTransfer implements Runnable {
	
	private final String TAG = "ServerFileTransfer";
	
	private final SocketUDT clientSocket;
	
	private ChatApplication mApplication;
	
	public ServerFileTransfer(SocketUDT newClientSocket, ChatApplication mChatApplication){
		clientSocket = newClientSocket;
		mApplication = mChatApplication;
		//filePathUnderSDRoot = "BigJava5.pdf"; //the path to a file should be passed through ctor
	}
	
	@Override
	public void run() {
		
		byte[] buffer;
		
		try{
			
			/**
			 * There are some initial checks before start to send the file:
			 * 1.check sd card is mounted, send -1 if not
			 * 2.File get back by calling getFileUri() is not null, send -2 if not
			 * 3.If everything is fine, send 0 as a ready signal
			 */
						
			//1. check if SD card is mounted or not
			if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
				buffer = new byte[4];
				buffer = Utility.toByteArray(-1); //send size as -1 if SD card is not mounted
				clientSocket.send(buffer);
				Log.d(TAG, "The SD card is not mounted, send -1 as initial handshake signal");
				clientSocket.close();
				return;
			}
			
			//2. if SD is present, get the File handle from the URI
			Uri fileUri = mApplication.getFileUri();
			File requestFile = FileUtility.getFile(mApplication, fileUri);
			//If error getting the File through Uri, send -2 as the 
			if (requestFile == null)
			{
				buffer = new byte[4];
				buffer = Utility.toByteArray(-2);
				clientSocket.send(buffer);
				Log.d(TAG, "File is null, send -2 as initial handshake signal");
				clientSocket.close();
				return;
			}
			
			//3.Everything is fine, send 0 as the initial handshake signal
			buffer = new byte[4];
			buffer = Utility.toByteArray(0);
			clientSocket.send(buffer);
			Log.d(TAG, "Check passed, send 0 as initial handshake signal");
			
			//4. send the file
			long mSize = requestFile.length();
	        Log.w(TAG, "The file size is " + mSize);				
			clientSocket.sendFile(requestFile, 0, mSize);
			Log.d(TAG, "File sending.");
			
			//5. finish sending file, close client socket (accept socket is still open)
			clientSocket.close();
			Log.d(TAG, "Socket closed.");			
			
		}
		catch (ExceptionUDT u){
			u.printStackTrace();
		}
		
	}	
	
}