package edu.usc.officeshare.server;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

public class FileServer implements Runnable{

	private final int port;
	private final ExecutorService pool;
	private final int MAX_CLIENT;
	
	private final static String TAG = "FileServer";
	
	public FileServer(int port, int max_client){
		this.port = port;
		this.MAX_CLIENT = max_client;
		pool = Executors.newFixedThreadPool(MAX_CLIENT);
	}	
	
	@Override
	public void run() {
		// This is the main of the FileServer listener thread
		try {
			final SocketUDT acceptorSocket = new SocketUDT(TypeUDT.STREAM);
			
			acceptorSocket.bind(new InetSocketAddress("0.0.0.0", port));
			acceptorSocket.listen(MAX_CLIENT);
			
			Log.d(TAG, "FileServer is ready at port: " + port);
			
			while (true){
				
				SocketUDT clientSocket = acceptorSocket.accept();
				clientSocket.setBlocking(true);
				Log.d(TAG,"New client connected");
				
				//when new client need the file, a new ServerFileTransfer thread is submitted to the pool
				pool.execute(new ServerFileTransfer(clientSocket));
				
				/*new Thread(new ServerFileTransfer(clientSocket)).start();
				
				Executors.newSingleThreadExecutor().submit(new Callable<Boolean>() {

					@Override
					public Boolean call(){
						return fileTransfer(clientSocket);
					}
					
				});*/
			}
			
			//acceptorSocket.close();
						
		} catch (ExceptionUDT e) {
			e.printStackTrace();
		}
	}

}
