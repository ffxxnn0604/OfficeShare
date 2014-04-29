package edu.usc.officeshare;

import org.alljoyn.bus.sample.chat.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import edu.usc.officeshare.server.FileServer;
import edu.usc.officeshare.util.OfficeShareConstants;

public class HostActivity extends Activity {

	private static final String TAG = "HostActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_host);

		//Start the FileServer listening thread
		Thread mFileServerThread = new Thread(new FileServer(OfficeShareConstants.DEFAULT_LISTEN_PORT));
		mFileServerThread.start();
		
		Log.d(TAG, "Server acceptor thread started");
		
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}*/

	/**
	 * A placeholder fragment containing a simple view.
	 */
	/*public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}*/

}
