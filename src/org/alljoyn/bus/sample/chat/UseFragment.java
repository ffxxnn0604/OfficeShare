package org.alljoyn.bus.sample.chat;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.usc.officeshare.client.ClientFileTransfer;
import edu.usc.officeshare.server.FileServer;
import edu.usc.officeshare.signal.FileInfo;
import edu.usc.officeshare.util.FileUtility;
import edu.usc.officeshare.util.OfficeShareConstants;
import edu.usc.officeshare.util.Utility;

public class UseFragment extends Fragment implements Observer{

    private static final String TAG = "chat.UseFragment";
    
    private ChatApplication mChatApplication = null;
    private Thread mFileServerThread = null;
    private Thread mFileClientThread = null;
    
    private ArrayAdapter<String> mHistoryList;
    
    private Button mJoinButton;
    private Button mLeaveButton;
    
    private Button mShareFile;
    private Button mRequestFile;
    
    private TextView mChannelName;      
    private TextView mChannelStatus;
    
    private TextView mFileURI;
    private TextView mFileName;
    private TextView mFileSize;
    
    private static final int READ_PDF_REQUEST_CODE = 40;
	    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView()");
		super.onCreateView(inflater, container, savedInstanceState);
		
		View mUseView = inflater.inflate(R.layout.fragment_use, container, false);
		
		mHistoryList = new ArrayAdapter<String>(this.getActivity(), android.R.layout.test_list_item);
        ListView hlv = (ListView) mUseView.findViewById(R.id.useHistoryList);
        hlv.setAdapter(mHistoryList);
        
        EditText messageBox = (EditText)mUseView.findViewById(R.id.useMessage);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                boolean handled = false;
            	if (actionId == EditorInfo.IME_ACTION_SEND){// && event.getAction() == KeyEvent.ACTION_UP) {
                	String message = view.getText().toString();
                    Log.i(TAG, "useMessage.onEditorAction(): got message " + message + ")");
    	            mChatApplication.newLocalUserMessage(message);
    	            view.setText("");
    	            handled = true;
                }
                return handled;
            }
        });
                
        mJoinButton = (Button)mUseView.findViewById(R.id.useJoin);
        mJoinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
            	ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.UseJoin);
            	mChatDialogFragment.show(getFragmentManager(), "usejoin");
            	
            	/*CharSequence text = "Hello toast!";
            	int duration = Toast.LENGTH_SHORT;

            	Toast toast = Toast.makeText(getActivity(), text, duration);
            	toast.show();*/
            	
            	//showDialog(DIALOG_JOIN_ID);
        	}
        });

        mLeaveButton = (Button)mUseView.findViewById(R.id.useLeave);
        mLeaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.UseLeave);
            	mChatDialogFragment.show(getFragmentManager(), "useleave");
                //showDialog(DIALOG_LEAVE_ID);
            }
        });
        
        
        mShareFile = (Button)mUseView.findViewById(R.id.shareFile);
        mShareFile.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*Log.i(TAG, "Share File button pressed");*/
				performOpenFile();//right now doesn't accept parameter, should be able to select file types
			}
		});
        
        mRequestFile = (Button)mUseView.findViewById(R.id.requestFile);
        mRequestFile.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getFileFromHost();				
			}
		});
        
        mChannelName = (TextView)mUseView.findViewById(R.id.useChannelName);
        mChannelStatus = (TextView)mUseView.findViewById(R.id.useChannelStatus);
        
        mFileURI = (TextView)mUseView.findViewById(R.id.file_uri);
        mFileName = (TextView)mUseView.findViewById(R.id.file_name);
        mFileSize = (TextView)mUseView.findViewById(R.id.file_size);
        
        /*
         * Keep a pointer to the Android Application class around.  We use this
         * as the Model for our MVC-based application.    Whenever we are started
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
        updateHistory();
        
        /*
         * Now that we're all ready to go, we are ready to accept notifications
         * from other components.
         */
        mChatApplication.addObserver(this);
        
        return mUseView;
	}
    
    
    /**
	 * Fires an intent to spin up the "file chooser" UI and select an image.
	 */
	private void performOpenFile() {

	    // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
	    // browser.
	    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

	    // Filter to only show results that can be "opened", such as a
	    // file (as opposed to a list of contacts or timezones)
	    intent.addCategory(Intent.CATEGORY_OPENABLE);

	    // Filter to show only pdf, using the pdf MIME data type.
	    // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
	    // To search for all documents available via installed storage providers,
	    // it would be "*/*".
	    /*switch (type){
		    case PDF:{*/
    	intent.setType("application/pdf");
    	startActivityForResult(intent, READ_PDF_REQUEST_CODE);
		 /*   	break;
		    }
		    case IMAGE:{
		    	intent.setType("image/*");
		    	startActivityForResult(intent, READ_IMAGE_REQUEST_CODE);
		    	break;
		    }
		    case MP3:{
		    	intent.setType("audio/mpeg");
		    	startActivityForResult(intent, READ_MP3_REQUEST_CODE);
		    	break;
		    }
		    default:{
		    	intent.setType("application/pdf");
		    	startActivityForResult(intent, READ_PDF_REQUEST_CODE);
		    	break;
		    }
	    }*/	    
	    
	}
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// The ACTION_OPEN_DOCUMENT intent was sent with the request code
	    // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
	    // response to some other intent, and the code below shouldn't run at all.
		
		if (resultCode == Activity.RESULT_OK){
			if (requestCode == READ_PDF_REQUEST_CODE){
				//open PDF activity
			}
			/*else if (requestCode == READ_IMAGE_REQUEST_CODE){
				//open image activity
			}
			else if (requestCode == READ_MP3_REQUEST_CODE){
				//open MP3 activity
			}*/
			else{
				//unsupported file type
			}
			
			// The document selected by the user won't be returned in the intent.
	        // Instead, a URI to that document will be contained in the return intent
	        // provided to this method as a parameter.
	        // Pull that URI using resultData.getData().
	        Uri uri = null;
	        if (data != null) {
	            uri = data.getData();
	            Log.i(TAG, "Uri: " + uri.toString());
	            mFileURI.setText(uri.toString());
	            
	            mChatApplication.setFileUri(uri);
	            
	            //We need to make sure before we inform other participants, we already has FileServerThread up and running
	            //check mFileServerThread, if null, we need to start the UDT file server thread
				if (mFileServerThread == null)
				{
					mFileServerThread = new Thread(new FileServer(OfficeShareConstants.DEFAULT_LISTEN_PORT, mChatApplication));
					mFileServerThread.start();
				}
				//TODO if mFileServerThread is not null, need to update the thread to open a different FILE
				else
				{
					
				}            
		        
		        getFileMetaData(uri);
		        File mFile = FileUtility.getFile(getActivity(), uri);
	            Uri mUri = FileUtility.getUri(mFile);

	            Intent intent = new Intent(getActivity(), com.artifex.mupdfdemo.MuPDFActivity.class);
	    		intent.setAction(Intent.ACTION_VIEW);
	    		//intent.setData(Uri.parse("content://com.estrongs.files/storage/sdcard0/2000camry.pdf"));
	    		intent.setData(mUri);
	    		startActivity(intent); 
	        }       
	        
	        
	        /*File mFile = FileUtility.getFile(mChatApplication, uri);
	        long mSize = mFile.length();
	        Log.w(TAG, "The file size is " + mSize);*/
	        
		}
		
	}
	
	private void getFileMetaData(Uri uri) {

	    // The query, since it only applies to a single document, will only return
	    // one row. There's no need to filter, sort, or select fields, since we want
	    // all fields for one document.
	    Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

	    try {
	    // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
	    // "if there's anything to look at, look at it" conditionals.
	        if (cursor != null && cursor.moveToFirst()) {

	            // Note it's called "Display Name".  This is
	            // provider-specific, and might not necessarily be the file name.
	            String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
	            Log.i(TAG, "Display Name: " + displayName);
	            mFileName.setText(displayName);

	            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
	            // If the size is unknown, the value stored is null.  But since an
	            // int can't be null in Java, the behavior is implementation-specific,
	            // which is just a fancy term for "unpredictable".  So as
	            // a rule, check if it's null before assigning to an int.  This will
	            // happen often:  The storage API allows for remote files, whose
	            // size might not be locally known.
	            String size = null;
	            long mSize = 0;
	            if (!cursor.isNull(sizeIndex)) {
	                // Technically the column stores an int, but cursor.getString()
	                // will do the conversion automatically.
	                size = cursor.getString(sizeIndex);
	                mSize = cursor.getInt(sizeIndex);
	            } else {
	                size = "Unknown";
	            }
	            Log.i(TAG, "Size: " + size);
	            mFileSize.setText(size);
	            
	            //now we need to get the old file info, and compare, if different, then broadcast
	            // TODO need to test file size is not null, also need to add uri to fileinfo
	            // TODO the default port number needs to be updated
	            String mIP = Utility.getIPAddress(true);
	            FileInfo mFileInfoNew = new FileInfo(mIP, OfficeShareConstants.DEFAULT_LISTEN_PORT, displayName, mSize);
	             
	            if (!mChatApplication.isSameFile(mFileInfoNew))
	            {
	            	mChatApplication.setFileInfo(mFileInfoNew);
	            	mChatApplication.broadcastFileInfo();
	            	Log.w(TAG, "broadcastFileInfo called!");
	            }
	            
	            
	        }
	    } finally {
	        cursor.close();
	    }
	}
	
	private void getFileFromHost()
	{
		//TODO start a clientFileTransfer thread to get the file from the host
		FileInfo mFileInfo = mChatApplication.getFileInfo();
		if (mFileInfo == null)
		{
			Log.w(TAG, "mFileInfo is null, request a manual update from the host, then display a dialog");
			mChatApplication.updateFileInfo(); //request manual update, next time we should have a non-null mFileInfo
			
			ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.NoFileInfo);
    		mChatDialogFragment.show(getFragmentManager(), "NoFileInfo");    	
			
			return;
		}
		else if (mFileInfo.fileServerIP.equalsIgnoreCase("0.0.0.0")){
			//Host hasn't select a file, only a 0 initialized FileInfo object is returned
			mChatApplication.updateFileInfo(); //request manual update, next time we should have a non-null mFileInfo
			
			ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.NoFileInfo);
    		mChatDialogFragment.show(getFragmentManager(), "NoFileInfo");    	
			
			return;
		}
		
		//if mFileInfo is not null and its content is not all 0, then we are OK to proceed and transfer the file from host
		Log.w(TAG, "FileServerIP is " + mFileInfo.fileServerIP + "FileServerPort is "+mFileInfo.fileServerPort
				+"FileName: " + mFileInfo.fileName + "FileSize: " + mFileInfo.fileSize);
		
		mFileClientThread = new Thread(new ClientFileTransfer(mFileInfo.fileServerIP, mFileInfo.fileServerPort, mChatApplication, mHandler));
		mFileClientThread.start();
						
		      
	}
    
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mChatApplication = (ChatApplication)getActivity().getApplication();
        mChatApplication.deleteObserver(this);
    	super.onDestroy();
 	}
    
    public static final int DIALOG_JOIN_ID = 0;
    public static final int DIALOG_LEAVE_ID = 1;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 2;

//    protected Dialog onCreateDialog(int id) {
//    	Log.i(TAG, "onCreateDialog()");
//        Dialog result = null;
//        switch(id) {
//        case DIALOG_JOIN_ID:
//	        { 
//	        	DialogBuilder builder = new DialogBuilder();
//	        	result = builder.createUseJoinDialog(this, mChatApplication);
//	        }        	
//        	break;
//        case DIALOG_LEAVE_ID:
//	        { 
//	        	DialogBuilder builder = new DialogBuilder();
//	        	result = builder.createUseLeaveDialog(this, mChatApplication);
//	        }
//	        break;
//        case DIALOG_ALLJOYN_ERROR_ID:
//	        { 
//	        	DialogBuilder builder = new DialogBuilder();
//	        	result = builder.createAllJoynErrorDialog(this, mChatApplication);
//	        }
//	        break;	        
//        }
//        return result;
//    }
    
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;
        
//        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
//            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
//            mHandler.sendMessage(message);
//        }
        
        if (qualifier.equals(ChatApplication.HISTORY_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.USE_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
        
        else{
        	Log.w(TAG, "Use Activity was notified with an irrelavent event: " + qualifier);
        }
    }
    
    private void updateHistory() {
        Log.i(TAG, "updateHistory()");
	    mHistoryList.clear();
	    List<String> messages = mChatApplication.getHistory();
        for (String message : messages) {
            mHistoryList.add(message);
        }
	    mHistoryList.notifyDataSetChanged();
    }
    
    private void updateChannelState() {
        Log.i(TAG, "updateHistory()");
    	AllJoynService.UseChannelState channelState = mChatApplication.useGetChannelState();
    	String name = mChatApplication.useGetChannelName();
    	if (name == null) {
    		name = "Not set";
    	}
    	else {
    		Log.w(TAG, "name is not null, it's " + name);
    	}
        mChannelName.setText(name);
        
        switch (channelState) {
        case IDLE:
            mChannelStatus.setText("Idle");
            mJoinButton.setEnabled(true);
            mLeaveButton.setEnabled(false);
            mShareFile.setEnabled(false);
            mRequestFile.setEnabled(false);
            break;
        case JOINEDSELF:
            mChannelStatus.setText("JoinedSelf");
            mJoinButton.setEnabled(false);
            mLeaveButton.setEnabled(true);
            mShareFile.setEnabled(true);
            mRequestFile.setEnabled(false);
            break;
        case JOINEDOTHER:
            mChannelStatus.setText("JoinedOther");
            mJoinButton.setEnabled(false);
            mLeaveButton.setEnabled(true);
            mShareFile.setEnabled(false);
            mRequestFile.setEnabled(true);
            break;
        }
    }
    
    //private boolean mShowErrorDialog = false;
    
    /**
     * An AllJoyn error has happened.  Since this activity pops up first we
     * handle the general errors.  We also handle our own errors.
     */
    private void alljoynError() {
    	if (mChatApplication.getErrorModule() == ChatApplication.Module.GENERAL ||
    		mChatApplication.getErrorModule() == ChatApplication.Module.USE) {
    		
    		//mShowErrorDialog = true;
    		ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.AllJoynError);
        	//only put the dialog into UseActivity's fragment stack, but not commit it (show it)
    		//the fragment is only committed when onResumeFragment is called (UseActivity is resumed)
    		//getFragmentManager().putFragment(null, "AllJoynError", mChatDialogFragment);
    		mChatDialogFragment.show(getFragmentManager(), "AllJoynError");
    		
    		//showDialog(DIALOG_ALLJOYN_ERROR_ID);
    	}
    	else{
    		Log.w(TAG, "Use activity was notidied with an error for other module!");
    	}
    }
    
    //just in case someone called startActivityForResult on UseActivity (This shouldn't happen in this app)
//    @Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//    	super.onActivityResult(requestCode, resultCode, data);
//    	mShowErrorDialog = true;
//    	Log.w(TAG, "onActivityResult() called!");
//    }
    
    @Override
	public void onResume() {
    	//Log.w(TAG, "onResume() called");
    	super.onResume();
//    	if (mShowErrorDialog){
//    		mShowErrorDialog = false;
//    		
//    		//The following code shouldn't be necessary, it's only called when flag (mShowErrorDialog) is set
//    		//but no fragment by the tag "AllJoynError" is in the framgnetManager (A possible error case?)
//    		if (getFragmentManager().findFragmentByTag("AllJoynError") == null){
//    			Log.w(TAG, "onResumeFragment(): flag is set, but no dialog was put on stack before!");
//    			ChatDialogFragment mChatDialogFragment = ChatDialogFragment.newInstance(DialogType.AllJoynError);
//    			mChatDialogFragment.show(getFragmentManager(), "AllJoynError");
//    		}
//    	}
    }
    
    //private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_HISTORY_CHANGED_EVENT = 0;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 1;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 2;
    private static final int HANDLE_FILE_COMPELTE_EVENT = 3;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // TODO add code to TabActivity, and ask it to popBackStack() from its getSupportFragmentManager()
//            case HANDLE_APPLICATION_QUIT_EVENT:
//	            {
//	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
//	                finish();
//	                break;
//	            }	             
            case HANDLE_HISTORY_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
                    updateHistory();
                    break;
                }
            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
	                updateChannelState();
	                break;
	            }
            case HANDLE_ALLJOYN_ERROR_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
	                alljoynError();
	                break;
	            }
            case HANDLE_FILE_COMPELTE_EVENT:
            	{
        		Toast toast = Toast.makeText(getActivity(), "File Complete!", Toast.LENGTH_SHORT);
            	toast.show();
            	
            	File dirSDRoot = Environment.getExternalStorageDirectory();
				File destFile = new File(dirSDRoot, mChatApplication.getFileInfo().fileName);
				Uri mUri = FileUtility.getUri(destFile);

	            Intent intent = new Intent(getActivity(), com.artifex.mupdfdemo.MuPDFActivity.class);
	    		intent.setAction(Intent.ACTION_VIEW);
	    		//intent.setData(Uri.parse("content://com.estrongs.files/storage/sdcard0/2000camry.pdf"));
	    		intent.setData(mUri);
	    		startActivity(intent); 
            		break;
                }            	
            default:
                break;
            }
        }
    };
    
}
