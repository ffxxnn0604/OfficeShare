package edu.usc.officeshare;

import org.alljoyn.bus.sample.chat.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import edu.usc.officeshare.client.ClientFileTransfer;

public class ClientActivity extends Activity {

	private static final String TAG = "ClientActivity";
	private static final int DEFAULT_LISTEN_PORT = 9000;
	private static final String DEFAULT_HOST = "192.168.0.5";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);

		//Start the ClientFile Transfer thread
		new Thread(new ClientFileTransfer(DEFAULT_HOST, DEFAULT_LISTEN_PORT)).start();
		Log.d(TAG, "Client thread started");
		
	}
	
}
