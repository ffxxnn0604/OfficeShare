package edu.usc.officeshare.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

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

	private SocketUDT clientSocket;

	public ClientFileTransfer(String host, int port) {
		// TODO Auto-generated constructor stub

		this.host = host;
		this.port = port;

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
		int fileNameLength;
		String fileName;

		try {

			clientSocket.setBlocking(true);

			clientSocket.connect(new InetSocketAddress(host, port));

			// Thread.sleep(1000);

			// 1. get the length of the filename
			buffer = new byte[4];
			clientSocket.receive(buffer);
			fileNameLength = Utility.byteToInt(buffer);
			Log.d(TAG,"The length of filename is " + fileNameLength);

			// 2. get the filename
			buffer = new byte[fileNameLength];
			clientSocket.receive(buffer);
			fileName = new String(buffer);
			Log.d(TAG,"The file name is "+ fileName);

			// 3. receive the request file size
			buffer = new byte[8];
			clientSocket.receive(buffer);
			long requestFileSize = Utility.byteToLong(buffer);
			Log.d(TAG,"The shared file has size " + requestFileSize);

			if (requestFileSize == 0) {
				// The requested file doesn't exist, close the socket.
				Log.d(TAG,"File doesn't exist, closing socket.");
				clientSocket.close();
			} else if (requestFileSize == -1) {
				Log.d(TAG,"The SD card is not mounted in the server phone, closing socket.");
				clientSocket.close();
			} else {
				// 4. The request file exist, open a new File handle and receive
				// the file, try to store to sd first, if not enough space, try internal storage
				File dirSDRoot = Environment.getExternalStorageDirectory();
				File destFile = new File(dirSDRoot, fileName);
				
				RandomAccessFile newFile = new RandomAccessFile(destFile, "rw");
				newFile.setLength(requestFileSize);
				newFile.close();

				clientSocket.receiveFile(destFile, 0, requestFileSize);

				// 5. close the socket
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
