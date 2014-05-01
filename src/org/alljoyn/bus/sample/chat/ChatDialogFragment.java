package org.alljoyn.bus.sample.chat;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ChatDialogFragment extends DialogFragment {
	
	private static final String TAG = "chat.DialogFrag";
	
	public static ChatDialogFragment newInstance(DialogType type) {
		ChatDialogFragment myFragment = new ChatDialogFragment();

	    Bundle args = new Bundle();
	    args.putSerializable("DialogType", type);
	    myFragment.setArguments(args);    

	    return myFragment;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreateDialog(savedInstanceState);
		DialogType type = (DialogType)getArguments().getSerializable("DialogType");
			
		ChatApplication mChatApplication = (ChatApplication)getActivity().getApplication();
		Activity mActivity = getActivity();
		
		Dialog result = null;
		
		switch(type){
			case UseJoin:
			{
				result = createUseJoinDialog(mActivity, mChatApplication);
			}
			break;
			case UseLeave:
			{
				result = createUseLeaveDialog(mActivity, mChatApplication);
			}
			break;
			case HostName:
			{
				result = createHostNameDialog(mActivity, mChatApplication);
			}
			break;
			case HostStart:
			{
				result = createHostStartDialog(mActivity, mChatApplication);
			}
			break;
			case HostStop:
			{
				result = createHostStopDialog(mActivity, mChatApplication);
			}
			break;
			case AllJoynError:
			{
				result = createAllJoynErrorDialog(mActivity, mChatApplication);
			}
			break;
			case NoFileInfo:
			{
				result = createNoFileInfoDialog(mActivity, mChatApplication);
			}
			break;
			default:
			{
				Log.e(TAG, "The DialogType is missing!");
			}
		}
		
		return result;
		
		//return super.onCreateDialog(savedInstanceState);
	}
	
	private Dialog createUseJoinDialog(final Activity activity, final ChatApplication application){
		
		//setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault);
		Log.i(TAG, "createUseJoinDialog()");
		
		//create the view based on the xml file
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.dialogfragment_usejoin, null);
		
		//set up the list view inside the view above
		ArrayAdapter<String> channelListAdapter = new ArrayAdapter<String>(activity, android.R.layout.test_list_item);
    	final ListView channelList = (ListView)view.findViewById(R.id.useJoinChannelList);
        channelList.setAdapter(channelListAdapter);
        
        //populate the list view with actual data
	    List<String> channels = application.getFoundChannels();
        for (String channel : channels) {
        	int lastDot = channel.lastIndexOf('.');
        	if (lastDot < 0) {
        		continue;
        	}
            channelListAdapter.add(channel.substring(lastDot + 1));
        }
	    channelListAdapter.notifyDataSetChanged();
		
	    //register the list view listener
	    channelList.setOnItemClickListener(new ListView.OnItemClickListener() {
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			String name = channelList.getItemAtPosition(position).toString();
				application.useSetChannelName(name);
				application.useJoinChannel();
				
    			dismiss();
    		}
    	});
		
		//use the builder to create the alert dialog		
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(view)	
			   .setTitle("Select a channel from list below")
		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dismiss();
					}
				});
		
		//create the dialog from builder, and return it
		return builder.create();
	}
	
    public Dialog createUseLeaveDialog(Activity activity, final ChatApplication application) {
    	
    	Log.i(TAG, "createUseLeaveDialog()");
    	
    	//use the builder to create the alert dialog		
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage("Are you sure you want to leave the channel?")
			   .setPositiveButton("Yes", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					application.useLeaveChannel();
	    			application.useSetChannelName("Not set");
	    			dismiss();
				}
				   
			   })
		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dismiss();
					}
				});
		
		//create the dialog from builder, and return it
		return builder.create();
    }
    
    public Dialog createHostNameDialog(Activity activity, final ChatApplication application) {
       	
    	Log.i(TAG, "createHostNameDialog()");
    	
    	//create the view based on the xml file
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.dialogfragment_hostnamedialog, null);
		
		//set up the EditText inside the view above
		final EditText channelName = (EditText)view.findViewById(R.id.hostNameChannel);
		channelName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_DONE){
					String name = v.getText().toString();
					application.hostSetChannelName(name);
					application.hostInitChannel();
					dismiss();
				}
				return true;
			}
		});
		
		InputFilter filter = new InputFilter(){

			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				// TODO Auto-generated method stub
				if (source instanceof SpannableStringBuilder) {
		            SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
		            for (int i = end - 1; i >= start; i--) { 
		                char currentChar = source.charAt(i);
		                 if (!Character.isLetterOrDigit(currentChar) && currentChar != '_') {    
		                     sourceAsSpannableBuilder.delete(i, i+1);
		                 }     
		            }
		            return source;
		        } else {
		            StringBuilder filteredStringBuilder = new StringBuilder();
		            for (int i = start; i < end; i++) { 
		                char currentChar = source.charAt(i);
		                if (Character.isLetterOrDigit(currentChar) || currentChar == '_') {    
		                    filteredStringBuilder.append(currentChar);
		                }     
		            }
		            return filteredStringBuilder.toString();
		        }
			}
			
		};
		
		channelName.setFilters(new InputFilter[] {filter});
		
		//use the builder to create the alert dialog		
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(view)	
			   .setTitle("Enter a channel name below")
			   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					String name = channelName.getText().toString();
					application.hostSetChannelName(name);
					application.hostInitChannel();
					dismiss();					
				}
			})
		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dismiss();
					}
				});
		
		//create the dialog from builder, and return it
		return builder.create();
    	
    }
    
    public Dialog createHostStartDialog(Activity activity, final ChatApplication application) {
    	
    	Log.i(TAG,"createHostStartDialog()");
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage("Are you sure you want to start the channel?")
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					application.hostStartChannel();
					dismiss();
				}
			})
    	      .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dismiss();
				}
			});
    	
    	return builder.create();
    }
    
    public Dialog createHostStopDialog(Activity activity, final ChatApplication application) {
    	
    	Log.i(TAG, "createHostStopDialog()");
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage("Are you sure you want to stop the channel?")
    		   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					application.hostStopChannel();
					dismiss();
				}
			})
			   .setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dismiss();
					
				}
			});    	
    	return builder.create();
    	
    }
    
    public Dialog createAllJoynErrorDialog(Activity activity, final ChatApplication application) {

    	Log.i(TAG, "createAllJoynErrorDialog()");
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setTitle("An error has been reported by AllJoyn:")
    		   .setMessage(application.getErrorString())
    		   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dismiss();
				}
			});
    	
    	return builder.create();
    	    	
    }
    
    public Dialog createNoFileInfoDialog(Activity activity, final ChatApplication application) {
    	
    	Log.i(TAG, "createNoFileInfoDialog()");
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage("No file has been selected by host, please wait and try later.")
    		   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dismiss();
				}
			});
    	
    	return builder.create();
    	
    }   
    
}
