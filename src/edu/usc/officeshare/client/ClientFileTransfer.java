package edu.usc.officeshare.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

import org.alljoyn.bus.sample.chat.ChatApplication;
import org.alljoyn.bus.sample.chat.FileInfo;

import android.os.Environment;
import android.util.Log;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

import edu.usc.officeshare.util.Utility;

public class ClientFileTransfer implements Runnable {

	private final String TAG = "ClientFileTransfer";

	private String host;

	private int port;
	
	private ChatApplication mApplication;
	
	private FileInfo mFileInfo;

	private SocketUDT clientSocket;

	public ClientFileTransfer(String host, int port, ChatApplication mChatApplication) {
		// TODO Auto-generated constructor stub

		this.host = host;
		this.port = port;
		this.mApplication = mChatApplication;

		try {
			clientSocket = new SocketUDT(TypeUDT.STREAM);
		} catch (ExceptionUDT e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] buffer;

		try {

			clientSocket.setBlocking(true);

			clientSocket.connect(new InetSocketAddress(host, port));
			
			mFileInfo = mApplication.getFileInfo();

			// 1. get the initial sending signal of this transfer
			buffer = new byte[4];
			clientSocket.receive(buffer);
			int mHandShake = Utility.byteToInt(buffer);
			Log.d(TAG,"The initial handshake signal is " + mHandShake);
			
			if (mHandShake == -1) {
				Log.d(TAG,"The SD card is not mounted in the server phone, closing socket.");
				clientSocket.close();
			} else if (mHandShake == -2) {
				Log.d(TAG,"The File returned by getFile from the Uri is null, closing socket.");
				clientSocket.close();
			} else if (mHandShake == 0) {
				Log.d(TAG,"The server thread is ready to transfer the file.");
				
				// 2. The request file is ready to be received, open a new File handle and receive
				// the file, try to store to sd first, if not enough space, try internal storage
				File dirSDRoot = Environment.getExternalStorageDirectory();
				File destFile = new File(dirSDRoot, mFileInfo.fileName);
				
				RandomAccessFile newFile = new RandomAccessFile(destFile, "rw");
				newFile.setLength(mFileInfo.fileSize);
				newFile.close();

				clientSocket.receiveFile(destFile, 0, mFileInfo.fileSize);

				// 3. close the socket
				clientSocket.close();
				Log.d(TAG,"The reqeust file is received and socket closed");
			}

		} catch (ExceptionUDT u) {
			u.printStackTrace();
		} catch (FileNotFoundException f) {
			f.printStackTrace();
		} catch (IOException i) {
			i.printStackTrace();
		}

	}

}
