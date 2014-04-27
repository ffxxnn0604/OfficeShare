package org.alljoyn.bus.sample.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class HostFragment extends Fragment implements Observer {

	private static final String TAG = "chat.HostFragment";
    
    private ChatApplication mChatApplication = null;
    
    private TextView mChannelName;
    private TextView mChannelStatus;
    
    private Button mSetNameButton;
    private Button mStartButton;
    private Button mStopButton;
    private Button mQuitButton;   
    
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView()");		
		super.onCreateView(inflater, container, savedInstanceState);
		
		View mHostView = inflater.inflate(R.layout.fragment_host, container, false);
		
		mChannelName = (TextView)mHostView.findViewById(R.id.hostChannelName);
        mChannelName.setText("");
        
        mChannelStatus = (TextView)mHostView.findViewById(R.id.hostChannelStatus);
        mChannelStatus.setText("Idle");
        
        mSetNameButton = (Button)mHostView.findViewById(R.id.hostSetName);
        mSetNameButton.setEnabled(true);
        mSetNameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
            	ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.HostName);
            	mChatDialogFragment.show(getFragmentManager(), "hostname"); 
            	//showDialog(DIALOG_SET_NAME_ID);
        	}
        });

        mStartButton = (Button)mHostView.findViewById(R.id.hostStart);
        mStartButton.setEnabled(false);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.HostStart);
            	mChatDialogFragment.show(getFragmentManager(), "hoststart"); 
            	//showDialog(DIALOG_START_ID);
            }
        });
        
        mStopButton = (Button)mHostView.findViewById(R.id.hostStop);
        mStopButton.setEnabled(false);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
            	ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.HostStop);
            	mChatDialogFragment.show(getFragmentManager(), "hoststop");            	
            	//showDialog(DIALOG_STOP_ID);
            }
        });
        
        /*
         * Keep a pointer to the Android Application class around.  We use this
         * as the Model for our MVC-based application.  Whenever we are started
         * we need to "check in" with the application so it can ensure that our
         * required services are running.
         */
        mChatApplication = (ChatApplication)getActivity().getApplication();
        mChatApplication.checkin();
        
        /*
         * Call down into the model to get its current state.  Since the model
         * outlives its Activities, this may actually be a lot of state and not
         * just empty.
         */
        updateChannelState();
        
        /*
         * Now that we're all ready to go, we are ready to accept notifications
         * from other components.
         */
        mChatApplication.addObserver(this);
        
        
        mQuitButton = (Button)mHostView.findViewById(R.id.hostQuit);
        mQuitButton.setEnabled(true);
        mQuitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mChatApplication.quit();
            }
        });		
		
		return mHostView;
	}
	
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mChatApplication = (ChatApplication)getActivity().getApplication();
        mChatApplication.deleteObserver(this);
        super.onDestroy();
 	}
	   
    
    static final int DIALOG_SET_NAME_ID = 0;
    static final int DIALOG_START_ID = 1;
    static final int DIALOG_STOP_ID = 2;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 3;
	
	@Override
	public synchronized void update(Observable o, Object arg) {
		
		Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;
        
//        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
//            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
//            mHandler.sendMessage(message);
//        }
        
        if (qualifier.equals(ChatApplication.HOST_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
        
        else {
        	Log.w(TAG, "Host Activity was notified with an irrelavent event: " + qualifier);
        }

	}
	
	private void updateChannelState() {
    	AllJoynService.HostChannelState channelState = mChatApplication.hostGetChannelState();
    	String name = mChatApplication.hostGetChannelName();
    	boolean haveName = true;
    	if (name == null) {
    		haveName = false;
    		name = "Not set";
    	}
        mChannelName.setText(name);
        switch (channelState) {
        case IDLE:
            mChannelStatus.setText("Idle");
            break;
        case NAMED:
            mChannelStatus.setText("Named");
            break;
        case BOUND:
            mChannelStatus.setText("Bound");
            break;
        case ADVERTISED:
            mChannelStatus.setText("Advertised");
            break;
        case CONNECTED:
            mChannelStatus.setText("Connected");
            break;
        default:
            mChannelStatus.setText("Unknown");
            break;
        }
        
        if (channelState == AllJoynService.HostChannelState.IDLE) {
            mSetNameButton.setEnabled(true);
            if (haveName) {
            	mStartButton.setEnabled(true);
            } else {
                mStartButton.setEnabled(false);
            }
            mStopButton.setEnabled(false);
        } else {
            mSetNameButton.setEnabled(false);
            mStartButton.setEnabled(false);
            mStopButton.setEnabled(true);
        }
    }
        
    private void alljoynError() {
    	if (mChatApplication.getErrorModule() == ChatApplication.Module.GENERAL ||
    		mChatApplication.getErrorModule() == ChatApplication.Module.HOST) {
    		
    		ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.AllJoynError);
        	mChatDialogFragment.show(getFragmentManager(), "AllJoynError");  
    		
    		//showDialog(DIALOG_ALLJOYN_ERROR_ID);
    	}
    	else {
    		Log.w(TAG, "Host activity was notified with an error for other module!");
    	}
    }
    
    //private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 0;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 1;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
	            // TODO add code in TabActivity to pop this fragment from the backstack
//            	case HANDLE_APPLICATION_QUIT_EVENT:
//	            {
//	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
//	                finish();
//	            }
//	            break; 
	            
	            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
	                updateChannelState();
	            }
                break;
            
	            case HANDLE_ALLJOYN_ERROR_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
	                alljoynError();
	            }
	            break;                
	            
	            default:
	            {
	            	Log.i(TAG, "mHandler.handleMessage(): UNKNOWN_EVENT");
	            }
	            break;
            }
        }
    };

}
