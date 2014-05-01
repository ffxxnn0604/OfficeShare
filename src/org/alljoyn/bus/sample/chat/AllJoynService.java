/*
 * Copyright (c) 2010-2011,2013, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.alljoyn.bus.sample.chat;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import edu.usc.officeshare.util.Utility;

public class AllJoynService extends Service implements Observer {
	private static final String TAG = "chat.AllJoynService";	

	/**
	 * We don't use the bindery to communicate between any client and this
	 * service so we return null.
	 */
	public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return null;
	}
	
	/**
	 * Our onCreate() method is called by the Android application framework
	 * when the service is first created.  We spin up a background thread
	 * to handle any long-lived requests (pretty much all AllJoyn calls that
	 * involve communication with remote processes) that need to be done and
	 * insinuate ourselves into the list of observers of the model so we can
	 * get event notifications. 
	 */
	@Override
	public void onCreate() {
        Log.i(TAG, "onCreate()");
        startBusThread(); //start the mBackgroundHandler 
        mChatApplication = (ChatApplication)getApplication();
        mChatApplication.addObserver(this);
        
        CharSequence title = "AllJoyn";
        CharSequence message = "Chat Channel Hosting Service.";
        /*Intent intent = new Intent(this, TabWidget.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, message, pendingIntent);
        notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;*/
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.seal)
                .setContentTitle(title)
                .setContentText(message)
                .setOngoing(true);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, TabActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(TabActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        //NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        //mNotificationManager.notify(mId, mBuilder.build());

        Log.i(TAG, "onCreate(): startForeground()");
        startForeground(NOTIFICATION_ID, mBuilder.build());
        
        /*
         * We have an AllJoyn handler thread running at this time, so take
         * advantage of the fact to get connected to the bus and start finding
         * remote channel instances in the background while the rest of the app
         * is starting up. 
         */
        mBackgroundHandler.connect();
        mBackgroundHandler.startDiscovery();
 	}
	
    private static final int NOTIFICATION_ID = 0xdefaced;
	
	/**
	 * Our onDestroy() is called by the Android application framework when it
	 * decides that our Service is no longer needed.  We tell our background
	 * thread to exit and remove ourselves from the list of observers of the
	 * model. 
	 */
    @Override
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mBackgroundHandler.cancelDiscovery();
        mBackgroundHandler.disconnect();
        stopBusThread();
        mChatApplication.deleteObserver(this);
 	}
    
	/**
	 * The method onStartCommand() is called by the Android application
	 * framework when a client explicitly starts a Service by calling 
	 * startService().  We expect that the only place this is going to be done
	 * is when the Android Application class for our application is created.
	 * The Application class provides our model in the sense of the MVC
	 * application we really are.
	 * 
	 * We return START_STICKY to enable us to be explicitly started and stopped
	 * which means that our Service will essentially run "forever" (or until 
	 * Android decides that we should die for resource management issues) since
	 * our Application class is left running as long as the process is left running.
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        return START_STICKY;
	}
	
    /**
     * A reference to a descendant of the Android Application class that is
     * acting as the Model of our MVC-based application.
     */
     private ChatApplication mChatApplication = null;
	
    /**
     * This is the event handler for the Observable/Observed design pattern.
     * Whenever an interesting event happens in our application, the Model (the
     * source of the event) notifies registered observers, resulting in this
     * method being called since we registered as an Observer in onCreate().
     * 
     * This method will be called in the context of the Model, which is, in 
     * turn the context of an event source.  This will either be the single
     * Android application framework thread if the source is one of the
     * Activities of the application or the Service.  It could also be in the
     * context of the Service background thread.  Since the Android Application
     * framework is a fundamentally single threaded thing, we avoid multithread
     * issues and deadlocks by immediately getting this event into a separate
     * execution in the context of the Service message pump.
     * 
     * We do this by taking the event from the calling component and queuing
     * it onto a "handler" in our Service and returning to the caller.  When
     * the calling component finishes what ever caused the event notification,
     * we expect the Android application framework to notice our pending
     * message and run our handler in the context of the single application
     * thread.
     * 
     * In reality, both events are executed in the context of the single 
     * Android thread.
     */
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;
        
        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.USE_JOIN_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_USE_JOIN_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.USE_LEAVE_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_USE_LEAVE_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.HOST_INIT_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_INIT_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.HOST_START_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_START_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.HOST_STOP_CHANNEL_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HOST_STOP_CHANNEL_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.OUTBOUND_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_OUTBOUND_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.BROADCAST_FILEINFO_EVENT)) {
        	Message message = mHandler.obtainMessage(HANDLE_BROADCAST_FILEINFO_EVENT);
        	mHandler.sendMessage(message);
        }
        
        else if (qualifier.equals(ChatApplication.UPDATE_FILEINFO_EVENT)) {
        	Message message = mHandler.obtainMessage(HANDLE_UPDATE_FILEINFO_EVENT);
        	mHandler.sendMessage(message);
        }
        
        else {
        	Log.w(TAG, "AllJoyn Server was notified with an irrelavent event: " + qualifier);
        }
    }
     
    /**
     * This is the Android Service message handler.  It runs in the context of the
     * main Android Service thread, which is also shared with Activities since 
     * Android is a fundamentally single-threaded system.
     * 
     * The important thing for us is to note that this thread cannot be blocked for
     * a significant amount of time or we risk the dreaded "force close" message.
     * We can run relatively short-lived operations here, but we need to run our
     * distributed system calls in a background thread.
     * 
     * This handler serves translates from UI-related events into AllJoyn events
     * and decides whether functions can be handled in the context of the
     * Android main thread or if they must be dispatched to a background thread
     * which can take as much time as needed to accomplish a task.
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
	            case HANDLE_APPLICATION_QUIT_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): APPLICATION_QUIT_EVENT");
		                mBackgroundHandler.leaveSession();
		                mBackgroundHandler.cancelAdvertise();
		                mBackgroundHandler.unbindSession();
		                mBackgroundHandler.releaseName();
		                mBackgroundHandler.exit();
		                stopSelf();
		            }
		            break;            
	            case HANDLE_USE_JOIN_CHANNEL_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): USE_JOIN_CHANNEL_EVENT");
		                mBackgroundHandler.joinSession();
		            }
		            break;
		        case HANDLE_USE_LEAVE_CHANNEL_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): USE_LEAVE_CHANNEL_EVENT");
		                mBackgroundHandler.leaveSession();
		            }
		            break;
		        case HANDLE_HOST_INIT_CHANNEL_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): HOST_INIT_CHANNEL_EVENT");
		            }
		            break;	            
		        case HANDLE_HOST_START_CHANNEL_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): HOST_START_CHANNEL_EVENT");
		                mBackgroundHandler.requestName();
		                mBackgroundHandler.bindSession();
		                mBackgroundHandler.advertise();
		            }
		            break;
		        case HANDLE_HOST_STOP_CHANNEL_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): HOST_STOP_CHANNEL_EVENT");
		                mBackgroundHandler.cancelAdvertise();
		                mBackgroundHandler.unbindSession();
		                mBackgroundHandler.releaseName();
		            }
		            break;
		        case HANDLE_OUTBOUND_CHANGED_EVENT:
		            {
		                Log.i(TAG, "mHandler.handleMessage(): OUTBOUND_CHANGED_EVENT");
		                mBackgroundHandler.sendMessages();
		            }
		            break;
		        case HANDLE_BROADCAST_FILEINFO_EVENT:
			        {
			        	Log.i(TAG, "mHandler.handlerMessage(): BROADCAST_FILEINFO_EVENT");
			        	mBackgroundHandler.broadcastFileInfo();
			        }
			        break;
		        case HANDLE_UPDATE_FILEINFO_EVENT:
			        {
			        	Log.i(TAG, "mHandler.handlerMessage(): UPDATE_FILEINFO_EVENT");
			        	mBackgroundHandler.updateFileInfo();
			        }
			        break;
	            default:
	                break;
            }
        }
    };
    
    /**
     * Value for the HANDLE_APPLICATION_QUIT_EVENT case observer notification handler. 
     */
    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    
    /**
     * Value for the HANDLE_USE_JOIN_CHANNEL_EVENT case observer notification handler. 
     */
    private static final int HANDLE_USE_JOIN_CHANNEL_EVENT = 1;
    
    /**
     * Value for the HANDLE_USE_LEAVE_CHANNEL_EVENT case observer notification handler. 
     */
    private static final int HANDLE_USE_LEAVE_CHANNEL_EVENT = 2;
    
    /**
     * Value for the HANDLE_HOST_INIT_CHANNEL_EVENT case observer notification handler. 
     */
    private static final int HANDLE_HOST_INIT_CHANNEL_EVENT = 3;
    
    /**
     * Value for the HANDLE_HOST_START_CHANNEL_EVENT case observer notification handler. 
     */
    private static final int HANDLE_HOST_START_CHANNEL_EVENT = 4;
    
    /**
     * Value for the HANDLE_HOST_STOP_CHANNEL_EVENT case observer notification handler. 
     */
    private static final int HANDLE_HOST_STOP_CHANNEL_EVENT = 5;
    
    /**
     * Value for the HANDLE_OUTBOUND_CHANGED_EVENT case observer notification handler. 
     */
    private static final int HANDLE_OUTBOUND_CHANGED_EVENT = 6;
    
    private static final int HANDLE_BROADCAST_FILEINFO_EVENT = 7;
    
    private static final int HANDLE_UPDATE_FILEINFO_EVENT = 8;
    
    /**
     * Enumeration of the states of the AllJoyn bus attachment.  This
     * lets us make a note to ourselves regarding where we are in the process
     * of preparing and tearing down the fundamental connection to the AllJoyn
     * bus.
     * 
     * This should really be a more private think, but for the sample we want
     * to show the user the states we are running through.  Because we are
     * really making a data hiding exception, and because we trust ourselves,
     * we don't go to any effort to prevent the UI from changing our state out
     * from under us.
     * 
     * There are separate variables describing the states of the client
     * ("use") and service ("host") pieces.
     */
    public static enum BusAttachmentState {
    	DISCONNECTED,	/** The bus attachment is not connected to the AllJoyn bus */ 
    	CONNECTED,		/** The  bus attachment is connected to the AllJoyn bus */
    	DISCOVERING		/** The bus attachment is discovering remote attachments hosting chat channels */
    }
    
    /**
     * The state of the AllJoyn bus attachment.
     */
    private BusAttachmentState mBusAttachmentState = BusAttachmentState.DISCONNECTED;
    
    /**
     * Enumeration of the states of a hosted chat channel.  This lets us make a
     * note to ourselves regarding where we are in the process of preparing
     * and tearing down the AllJoyn pieces responsible for providing the chat
     * service.  In order to be out of the IDLE state, the BusAttachment state
     * must be at least CONNECTED.
     */
    public static enum HostChannelState {
    	IDLE,	        /** There is no hosted chat channel */ 
    	NAMED,		    /** The well-known name for the channel has been successfully acquired */
    	BOUND,			/** A session port has been bound for the channel */
    	ADVERTISED,	    /** The bus attachment has advertised itself as hosting an chat channel */
    	CONNECTED       /** At least one remote device has connected to a session on the channel */
    }
    
    /**
     * The state of the AllJoyn components responsible for hosting an chat channel.
     */
    private HostChannelState mHostChannelState = HostChannelState.IDLE;
    
    /**
     * Enumeration of the states of a hosted chat channel.  This lets us make a
     * note to ourselves regarding where we are in the process of preparing
     * and tearing down the AllJoyn pieces responsible for providing the chat
     * service.  In order to be out of the IDLE state, the BusAttachment state
     * must be at least CONNECTED.
     */
    public static enum UseChannelState {
    	IDLE,	        /** There is no used chat channel */ 
    	JOINEDSELF,		/** The session for the channel has been successfully joined */
    	JOINEDOTHER		/** Joined a session hosted somewhere else*/
    }
    
    /**
     * The state of the AllJoyn components responsible for hosting an chat channel.
     */
    private UseChannelState mUseChannelState = UseChannelState.IDLE;
    
    /**
     * This is the AllJoyn background thread handler class.  AllJoyn is a
     * distributed system and must therefore make calls to other devices over
     * networks.  These calls may take arbitrary amounts of time.  The Android
     * application framework is fundamentally single-threaded and so the main
     * Service thread that is executing in our component is the same thread as
     * the ones which appear to be executing the user interface code in the
     * other Activities.  We cannot block this thread while waiting for a
     * network to respond, so we need to run our calls in the context of a
     * background thread.  This is the class that provides that background
     * thread implementation.
     *
     * When we need to do some possibly long-lived task, we just pass a message
     * to an object implementing this class telling it what needs to be done.
     * There are two main parts to this class:  an external API and the actual
     * handler.  In order to make life easier for callers, we provide API
     * methods to deal with the actual message passing, and then when the
     * handler thread is executing the desired method, it calls out to an 
     * implementation in the enclosing class.  For example, in order to perform
     * a connect() operation in the background, the enclosing class calls
     * BackgroundHandler.connect(); and the result is that the enclosing class
     * method doConnect() will be called in the context of the background
     * thread.
     */
    private final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }
        
        /**
         * Exit the background handler thread.  This will be the last message
         * executed by an instance of the handler.
         */
        public void exit() {
            Log.i(TAG, "mBackgroundHandler.exit()");
        	Message msg = mBackgroundHandler.obtainMessage(EXIT);
            mBackgroundHandler.sendMessage(msg);
        }
        
        /**
         * Connect the application to the Alljoyn bus attachment.  We expect
         * this method to be called in the context of the main Service thread.
         * All this method does is to dispatch a corresponding method in the
         * context of the service worker thread.
         */
        public void connect() {
            Log.i(TAG, "mBackgroundHandler.connect()");
        	Message msg = mBackgroundHandler.obtainMessage(CONNECT);
            mBackgroundHandler.sendMessage(msg);
        }
        
        /**
         * Disonnect the application from the Alljoyn bus attachment.  We
         * expect this method to be called in the context of the main Service
         * thread.  All this method does is to dispatch a corresponding method
         * in the context of the service worker thread.
         */
        public void disconnect() {
            Log.i(TAG, "mBackgroundHandler.disconnect()");
        	Message msg = mBackgroundHandler.obtainMessage(DISCONNECT);
            mBackgroundHandler.sendMessage(msg);
        }

        /**
         * Start discovering remote instances of the application.  We expect
         * this method to be called in the context of the main Service thread.
         * All this method does is to dispatch a corresponding method in the
         * context of the service worker thread.
         */
        public void startDiscovery() {
            Log.i(TAG, "mBackgroundHandler.startDiscovery()");
        	Message msg = mBackgroundHandler.obtainMessage(START_DISCOVERY);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        /**
         * Stop discovering remote instances of the application.  We expect
         * this method to be called in the context of the main Service thread.
         * All this method does is to dispatch a corresponding method in the
         * context of the service worker thread.
         */
        public void cancelDiscovery() {
            Log.i(TAG, "mBackgroundHandler.stopDiscovery()");
        	Message msg = mBackgroundHandler.obtainMessage(CANCEL_DISCOVERY);
        	mBackgroundHandler.sendMessage(msg);
        }

        public void requestName() {
            Log.i(TAG, "mBackgroundHandler.requestName()");
        	Message msg = mBackgroundHandler.obtainMessage(REQUEST_NAME);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void releaseName() {
            Log.i(TAG, "mBackgroundHandler.releaseName()");
        	Message msg = mBackgroundHandler.obtainMessage(RELEASE_NAME);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void bindSession() {
            Log.i(TAG, "mBackgroundHandler.bindSession()");
        	Message msg = mBackgroundHandler.obtainMessage(BIND_SESSION);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void unbindSession() {
            Log.i(TAG, "mBackgroundHandler.unbindSession()");
        	Message msg = mBackgroundHandler.obtainMessage(UNBIND_SESSION);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void advertise() {
            Log.i(TAG, "mBackgroundHandler.advertise()");
        	Message msg = mBackgroundHandler.obtainMessage(ADVERTISE);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void cancelAdvertise() {
            Log.i(TAG, "mBackgroundHandler.cancelAdvertise()");
        	Message msg = mBackgroundHandler.obtainMessage(CANCEL_ADVERTISE);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void joinSession() {
            Log.i(TAG, "mBackgroundHandler.joinSession()");
        	Message msg = mBackgroundHandler.obtainMessage(JOIN_SESSION);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void leaveSession() {
            Log.i(TAG, "mBackgroundHandler.leaveSession()");
        	Message msg = mBackgroundHandler.obtainMessage(LEAVE_SESSION);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void sendMessages() {
            Log.i(TAG, "mBackgroundHandler.sendMessages()");
        	Message msg = mBackgroundHandler.obtainMessage(SEND_MESSAGES);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void broadcastFileInfo(){
        	Log.i(TAG, "mBackgroundHandler.sendBroadcastFileInfo()");
        	Message msg = mBackgroundHandler.obtainMessage(BROADCAST_FILEINFO);
        	mBackgroundHandler.sendMessage(msg);
        }
        
        public void updateFileInfo(){
        	Log.i(TAG, "mBackgroundHandler.updateFileInfo()");
        	Message msg = mBackgroundHandler.obtainMessage(UPDATE_FILEINFO);
        	mBackgroundHandler.sendMessage(msg);
        }
                 
        /**
         * The message handler for the worker thread that handles background
         * tasks for the AllJoyn bus.
         */
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CONNECT:
	            doConnect();
            	break;
	        case DISCONNECT:
		        doDisconnect();
		    	break;
            case START_DISCOVERY:
	            doStartDiscovery();
            	break;
	        case CANCEL_DISCOVERY:
		        doStopDiscovery();
		    	break;
	        case REQUEST_NAME:
		        doRequestName();
		    	break;
	        case RELEASE_NAME:
		        doReleaseName();
		    	break;		
	        case BIND_SESSION:
		        doBindSession();
		    	break;
	        case UNBIND_SESSION:
		        doUnbindSession();
		        break;
	        case ADVERTISE:
		        doAdvertise();
		    	break;
	        case CANCEL_ADVERTISE:
		        doCancelAdvertise();		        
		    	break;	
	        case JOIN_SESSION:
		        doJoinSession();
		    	break;
	        case LEAVE_SESSION:
		        doLeaveSession();
		        break;
	        case SEND_MESSAGES:
		        doSendMessages();
		        break;
	        case EXIT:
                getLooper().quit();
                break;
	        case BROADCAST_FILEINFO:
	        	doBroadcastFileInfo();
	        	break;
	        case UPDATE_FILEINFO:
	        	doUpdateFileInfo();
	        	break;
		    default:
		    	break;
            }
        }
    }
    
    private static final int EXIT = 1;
    private static final int CONNECT = 2;
    private static final int DISCONNECT = 3;
    private static final int START_DISCOVERY = 4;
    private static final int CANCEL_DISCOVERY = 5;
    private static final int REQUEST_NAME = 6;
    private static final int RELEASE_NAME = 7;
    private static final int BIND_SESSION = 8;
    private static final int UNBIND_SESSION = 9;
    private static final int ADVERTISE = 10;
    private static final int CANCEL_ADVERTISE = 11;
    private static final int JOIN_SESSION = 12;
    private static final int LEAVE_SESSION = 13;
    private static final int SEND_MESSAGES = 14;
    private static final int BROADCAST_FILEINFO = 15;
    private static final int UPDATE_FILEINFO = 16;
    
    /**
     * The instance of the AllJoyn background thread handler.  It is created
     * when Android decides the Service is needed and is called from the
     * onCreate() method.  When Android decides our Service is no longer 
     * needed, it will call onDestroy(), which spins down the thread.
     */
    private BackgroundHandler mBackgroundHandler = null;
    
    /**
     * Since basically our whole reason for being is to spin up a thread to
     * handle long-lived remote operations, we provide this method to do so.
     */
    private void startBusThread() {
        HandlerThread busThread = new HandlerThread("BackgroundHandler");
        busThread.start();
    	mBackgroundHandler = new BackgroundHandler(busThread.getLooper());
    }
    
    /**
     * When Android decides that our Service is no longer needed, we need to
     * tear down the thread that is servicing our long-lived remote operations.
	 * This method does so. 
     */
    private void stopBusThread() {
        mBackgroundHandler.exit();
    }
    
    /**
     * The bus attachment is the object that provides AllJoyn services to Java
     * clients.  Pretty much all communication with AllJoyn is going to go through
     * this object.
     */
    private BusAttachment mBus  = new BusAttachment(ChatApplication.PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);
    
    /**
     * The well-known name prefix which all bus attachments hosting a channel
     * will use.  The NAME_PREFIX and the channel name are composed to give
     * the well-known name a hosting bus attachment will request and 
     * advertise.
     */
    private static final String NAME_PREFIX_SIGNAL= "org.alljoyn.bus.samples.chat";
    
    private static final String NAME_PREFIX_PROPERTY = "org.alljoyn.bus.FileInfo";
    
	/**
	 * The well-known session port used as the contact port for the chat service.
	 */
    private static final short CONTACT_PORT = 27;
    
    /**
     * The object path used to identify the service "location" in the bus
     * attachment.
     */
    private static final String SIGNAL_OBJECT_PATH = "/chatService";
    
    private static final String PROPERTY_OBJECT_PATH = "/fileInfoProperty";
    
    /**
     * The ChatBusListener is a class that listens to the AllJoyn bus for
     * notifications corresponding to the existence of events happening out on
     * the bus.  We provide one implementation of our listener to the bus
     * attachment during the connect(). 
     */
    private class ChatBusListener extends BusListener {
   		/**
		 * This method is called when AllJoyn discovers a remote attachment
		 * that is hosting an chat channel.  We expect that since we only
		 * do a findAdvertisedName looking for instances of the chat
		 * well-known name prefix we will only find names that we know to
		 * be interesting.  When we find a remote application that is
		 * hosting a channel, we add its channel name it to the list of
		 * available channels selectable by the user.
         *
         * In the class documentation for the BusListener note that it is a
         * requirement for this method to be multithread safe.  This is
         * accomplished by the use of a monitor on the ChatApplication as
         * exemplified by the synchronized attribute of the addFoundChannel
         * method there.
		 */
		public void foundAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.foundAdvertisedName(" + name + ")");
			
            if(name.contains(NAME_PREFIX_SIGNAL)){
            	ChatApplication application = (ChatApplication)getApplication();
    			application.addFoundChannel(name);
            }
            else if(name.contains(NAME_PREFIX_PROPERTY)){
            	// we will only be here if we're a client joining a remote host, and called findAdvertisedName() for the property interface before
            	assert(mUseSessionId != -1);
            	ProxyBusObject mProxyObj = mBus.getProxyBusObject(name, PROPERTY_OBJECT_PATH, mUseSessionId, new Class<?>[] {FileInfoInterface.class});
                mFileInfoPropertyInterface = mProxyObj.getInterface(FileInfoInterface.class);
                assert(mFileInfoPropertyInterface != null);
                Log.w(TAG, "mFileInfoPropertyInterface is no longer null!");
            }           
            
		}
		
   		/**
		 * This method is called when AllJoyn decides that a remote bus
		 * attachment that is hosting an chat channel is no longer available.
		 * When we lose a remote application that is hosting a channel, we
		 * remove its name from the list of available channels selectable
		 * by the user.  
         *
         * In the class documentation for the BusListener note that it is a
         * requirement for this method to be multithread safe.  This is
         * accomplished by the use of a monitor on the ChatApplication as
         * exemplified by the synchronized attribute of the removeFoundChannel
         * method there.
		 */
		public void lostAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.lostAdvertisedName(" + name + ")");
            
            if(name.contains(NAME_PREFIX_SIGNAL)) {
            	ChatApplication application = (ChatApplication)getApplication();
    			application.removeFoundChannel(name);
            }
            else if(name.contains(NAME_PREFIX_PROPERTY)) {
            	//TODO if the PROPERTY_INTERFACE is lost, then the host left the session, there might be something we need to do
            	mFileInfoPropertyInterface = null;
            }			
		}
    }
    
    /**
     * An instance of an AllJoyn bus listener that knows what to do with
     * foundAdvertisedName and lostAdvertisedName notifications.  Although
     * we often use the anonymous class idiom when talking to AllJoyn, the
     * bus listener works slightly differently and it is better to use an
     * explicitly declared class in this case.
     */
    private ChatBusListener mBusListener = new ChatBusListener();
    
    /**
     * The session identifier of the "host" session that the application
     * provides for remote devices.  Set to -1 if not connected.
     */
    int mHostSessionId = -1;
    
    /**
     * A flag indicating that the application has joined a chat channel that
     * it is hosting.  See the long comment in doJoinSession() for a
     * description of this rather non-intuitively complicated case.
     */
    boolean mJoinedToSelf = false;
    
    /**
     * This is the interface over which the chat messages will be sent in
     * the case where the application is joined to a chat channel hosted
     * by itself.  See the long comment in doJoinSession() for a
     * description of this rather non-intuitively complicated case.
     */
    ChatInterface mHostChatInterface = null;
    
    /**
     * This is the interface over which the chat messages will be sent.
     */
    ChatInterface mChatInterface = null;
    
    /**
     * This is the interface over which FileInfo is retrieved
     */
    FileInfoInterface mFileInfoPropertyInterface = null;
    
    /**
     * Implementation of the functionality related to connecting our app
     * to the AllJoyn bus.  We expect that this method will only be called in
     * the context of the AllJoyn bus handler thread; and while we are in the
     * DISCONNECTED state.
     */
    private void doConnect() {
        Log.i(TAG, "doConnect()");
        org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
    	assert(mBusAttachmentState == BusAttachmentState.DISCONNECTED);
    	mBus.useOSLogging(true);
    	mBus.setDebugLevel("ALLJOYN_JAVA", 7);
    	mBus.registerBusListener(mBusListener);
    	
        /* 
         * To make a service available to other AllJoyn peers, first
         * register a BusObject with the BusAttachment at a specific
         * object path.  Our service is implemented by the ChatService
         * BusObject found at the "/chatService" object path.
         */
        Status status = mBus.registerBusObject(mChatService, SIGNAL_OBJECT_PATH);
        if (Status.OK != status) {
    		mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to register the chat bus object: (" + status + ")");
        	return;
        }              
    	
    	status = mBus.connect();
    	if (status != Status.OK) {
    		mChatApplication.alljoynError(ChatApplication.Module.GENERAL, "Unable to connect to the bus: (" + status + ")");
        	return;
    	}
    	
        status = mBus.registerSignalHandlers(this);
    	if (status != Status.OK) {
    		mChatApplication.alljoynError(ChatApplication.Module.GENERAL, "Unable to register signal handlers: (" + status + ")");
        	return;
    	}
        
    	mBusAttachmentState = BusAttachmentState.CONNECTED;
    }  
    
    private void doRegisterProperty() {
    	Log.i(TAG, "doRegisterProperty()");
    	
    	assert(mBusAttachmentState == BusAttachmentState.DISCONNECTED);
    	Status status = mBus.registerBusObject(mFileInfoProperty, PROPERTY_OBJECT_PATH);
        if (Status.OK != status) {
    		mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to register the FileInfo bus object: (" + status + ")");
        	return;
        }     
        
    }
    
    /**
     * Implementation of the functionality related to disconnecting our app
     * from the AllJoyn bus.  We expect that this method will only be called
     * in the context of the AllJoyn bus handler thread.  We expect that this
     * method will only be called in the context of the AllJoyn bus handler
     * thread; and while we are in the CONNECTED state.
     */
    private boolean doDisconnect() {
        Log.i(TAG, "doDisonnect()");
    	assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
    	mBus.unregisterBusListener(mBusListener);
    	mBus.disconnect();
		mBusAttachmentState = BusAttachmentState.DISCONNECTED;
    	return true;
    }
    
    /**
     * Implementation of the functionality related to discovering remote apps
     * which are hosting chat channels.  We expect that this method will only
     * be called in the context of the AllJoyn bus handler thread; and while
     * we are in the CONNECTED state.  Since this is a core bit of functionality
     * for the "use" side of the app, we always do this at startup.
     */
    private void doStartDiscovery() {
        Log.i(TAG, "doStartDiscovery()");
    	assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
      	Status status = mBus.findAdvertisedName(NAME_PREFIX_SIGNAL);
    	if (status == Status.OK) {
        	mBusAttachmentState = BusAttachmentState.DISCOVERING;
        	return;
    	} else {
    		mChatApplication.alljoynError(ChatApplication.Module.USE, "Unable to start finding advertised names: (" + status + ")");
        	return;
    	}
    }
    
    /**
     * Implementation of the functionality related to stopping discovery of
     * remote apps which are hosting chat channels.
     */
    private void doStopDiscovery() {
        Log.i(TAG, "doStopDiscovery()");
    	assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
      	mBus.cancelFindAdvertisedName(NAME_PREFIX_SIGNAL);
      	
      	mBusAttachmentState = BusAttachmentState.CONNECTED;
    }
       
    /**
     * Implementation of the functionality related to requesting a well-known
     * name from an AllJoyn bus attachment.
     */
    private void doRequestName() {
        Log.i(TAG, "doRequestName()");
    	
        /*
         * In order to request a name, the bus attachment must at least be
         * connected.
         */
        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
    	assert (stateRelation >= 0);
    	
    	/*
    	 * We depend on the user interface and model to work together to not
    	 * get this process started until a valid name is set in the channel name.
    	 */
    	String wellKnownName = NAME_PREFIX_SIGNAL + "." + mChatApplication.hostGetChannelName();
        Status status = mBus.requestName(wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
        if (status == Status.OK) {
          	mHostChannelState = HostChannelState.NAMED;
          	mChatApplication.hostSetChannelState(mHostChannelState);
        } else {
    		mChatApplication.alljoynError(ChatApplication.Module.USE, "Unable to acquire well-known name: (" + status + ")");
        }
    }
    
    private void doRequestNameAndAdvertiseProperty() {
    	Log.i(TAG, "doRequestNameProperty()");
    	
    	assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
    	
    	int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        String wellKnownName = NAME_PREFIX_PROPERTY + "." + mChatApplication.hostGetChannelName();
    	Status status = mBus.requestName(wellKnownName, flag);
        if (status == Status.OK) {
        	/*
        	 * If we successfully obtain a well-known name from the bus 
        	 * advertise the same well-known name
        	 */
        	status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
            if (status != Status.OK) {
            	 /*
                 * If we are unable to advertise the name, release
                 * the name from the local bus.
                 */
                status = mBus.releaseName(wellKnownName);
                mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to advertise the FileInfo bus object: (" + status + ")");
            	return;
            }
        } 
    	
    }
    
    /**
     * Implementation of the functionality related to releasing a well-known
     * name from an AllJoyn bus attachment.
     */
    private void doReleaseName() {
        Log.i(TAG, "doReleaseName()");
        
        /*
         * In order to release a name, the bus attachment must at least be
         * connected.
         */
        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
    	assert (stateRelation >= 0);
    	assert(mBusAttachmentState == BusAttachmentState.CONNECTED || mBusAttachmentState == BusAttachmentState.DISCOVERING);
    	
    	/*
    	 * We need to progress monotonically down the hosted channel states
    	 * for sanity.
    	 */
    	assert(mHostChannelState == HostChannelState.NAMED);
    	
    	/*
    	 * We depend on the user interface and model to work together to not
    	 * change the name out from under us while we are running.
    	 */
    	String wellKnownName = NAME_PREFIX_SIGNAL + "." + mChatApplication.hostGetChannelName();

    	/*
    	 * There's not a lot we can do if the bus attachment refuses to release
    	 * the name.  It is not a fatal error, though, if it doesn't.  This is
    	 * because bus attachments can have multiple names.
    	 */
    	mBus.releaseName(wellKnownName);
    	
    	//if we are the host leaving the channel, also release the property name from the bus
    	if(mJoinedToSelf){
    		wellKnownName = NAME_PREFIX_PROPERTY + "." + mChatApplication.hostGetChannelName();
    		mBus.releaseName(wellKnownName);
    	}
    	mHostChannelState = HostChannelState.IDLE;
      	mChatApplication.hostSetChannelState(mHostChannelState);
    }
    
    /**
     * Implementation of the functionality related to binding a session port
     * to an AllJoyn bus attachment.
     */
    private void doBindSession() {
        Log.i(TAG, "doBindSession()");
        
        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
        
        Status status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
            /**
             * This method is called when a client tries to join the session
             * we have bound.  It asks us if we want to accept the client into
             * our session.
             *
             * In the class documentation for the SessionPortListener note that
             * it is a requirement for this method to be multithread safe.
             * Since we never access any shared state, this requirement is met.
             */
        	public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                Log.i(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
        	
                /*
        		 * Accept anyone who can get our contact port correct.
        		 */
        		if (sessionPort == CONTACT_PORT) {
        			return true;
        		}
        		return false;
            }
            
            /**
             * If we return true in acceptSessionJoiner, we admit a new client
             * into our session.  The session does not really exist until a 
             * client joins, at which time the session is created and a session
             * ID is assigned.  This method communicates to us that this event
             * has happened, and provides the new session ID for us to use.
             *
             * In the class documentation for the SessionPortListener note that
             * it is a requirement for this method to be multithread safe.
             * Since we never access any shared state, this requirement is met.
             * 
             * See comments in joinSession for why the hosted chat interface is
             * created here. 
             */
            public void sessionJoined(short sessionPort, int id, String joiner) {
                Log.i(TAG, "SessionPortListener.sessionJoined(" + sessionPort + ", " + id + ", " + joiner + ")");
                mHostSessionId = id;
                SignalEmitter emitter = new SignalEmitter(mChatService, id, SignalEmitter.GlobalBroadcast.Off);
                mHostChatInterface = emitter.getInterface(ChatInterface.class);
            }             
        });
        
        if (status == Status.OK) {
        	mHostChannelState = HostChannelState.BOUND;
          	mChatApplication.hostSetChannelState(mHostChannelState);
        } else {
    		mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to bind session contact port: (" + status + ")");
        	return;
        }
    }
    
    /**
     * Implementation of the functionality related to un-binding a session port
     * from an AllJoyn bus attachment.
     */
    private void doUnbindSession() {
        Log.i(TAG, "doUnbindSession()");
        
        /*
         * There's not a lot we can do if the bus attachment refuses to unbind
         * our port.
         */
     	mBus.unbindSessionPort(CONTACT_PORT);
        mHostChatInterface = null;
     	mHostChannelState = HostChannelState.NAMED;
      	mChatApplication.hostSetChannelState(mHostChannelState);
    }
    
    
    
    /**
     * Implementation of the functionality related to advertising a service on
     * an AllJoyn bus attachment.
     */
    private void doAdvertise() {
        Log.i(TAG, "doAdvertise()");
        
       	/*
    	 * We depend on the user interface and model to work together to not
    	 * change the name out from under us while we are running.
    	 */
    	String wellKnownName = NAME_PREFIX_SIGNAL + "." + mChatApplication.hostGetChannelName();        
        Status status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        
        if (status == Status.OK) {
        	mHostChannelState = HostChannelState.ADVERTISED;
          	mChatApplication.hostSetChannelState(mHostChannelState);
        } else {
    		mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to advertise well-known name: (" + status + ")");
        	return;
        }
    }
    

    
    /**
     * Implementation of the functionality related to canceling an advertisement
     * on an AllJoyn bus attachment.
     */
    private void doCancelAdvertise() {
        Log.i(TAG, "doCancelAdvertise()");
        
       	/*
    	 * We depend on the user interface and model to work together to not
    	 * change the name out from under us while we are running.
    	 */
    	String wellKnownName = NAME_PREFIX_SIGNAL + "." + mChatApplication.hostGetChannelName();        
        Status status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        
        if (status != Status.OK) {
    		mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to cancel advertisement of well-known name for signal: (" + status + ")");
        	return;
        }
        
        //only cancel advertise of the property interface if we're the host and we joined to our own session (i.e. we advertised the property interface)
        if (mJoinedToSelf){
        	wellKnownName = NAME_PREFIX_PROPERTY + "." + mChatApplication.hostGetChannelName();
        	status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        	
        	if (status != Status.OK) {
        		mChatApplication.alljoynError(ChatApplication.Module.HOST, "Unable to cancel advertisement of well-known name for property: (" + status + ")");
            	return;
        	}
        }
        
        /*
         * There's not a lot we can do if the bus attachment refuses to cancel
         * our advertisement, so we don't bother to even get the status.
         */
     	mHostChannelState = HostChannelState.BOUND;
      	mChatApplication.hostSetChannelState(mHostChannelState);
    }
    
    /**
     * Implementation of the functionality related to joining an existing
     * local or remote session.
     */
    private void doJoinSession() {
        Log.i(TAG, "doJoinSession()");
        
        /*
         * There is a relatively non-intuitive behavior of multi-point sessions
         * that one needs to grok in order to understand the code below.  The
         * important thing to understand is that there can be only one end-point
         * for a multi-point session in a particular bus attachment.  This
         * end-point can be created explicitly by a call to joinSession() or
         * implicitly by a call to bindSessionPort().  An attempt to call
         * joinSession() on a session port we have created with bindSessionPort()
         * will result in an error.
         * 
         * When we call bindSessionPort(), we do an implicit joinSession() and
         * thus signals (which correspond to our chat messages) will begin to
         * flow from the hosted chat channel as soon as we begin to host a
         * corresponding session.
         * 
         * To achieve sane user interface behavior, we need to block those
         * signals from the implicit join done by the bind until our user joins
         * the bound chat channel.  If we do not do this, the chat messages
         * from the chat channel hosted by the application will appear in the
         * chat channel joined by the application.
         *
         * Since the messages flow automatically, we can accomplish this by
         * turning a filter on and off in the chat signal handler.  So if we
         * detect that we are hosting a channel, and we find that we want to
         * join the hosted channel we turn the filter off.
         * 
         * We also need to be able to send chat messages to the hosted channel.
         * This means we need to point the mChatInterface at the session ID of
         * the hosted session.  There is another complexity here since the
         * hosted session doesn't exist until a remote session has joined.
         * This means that we don't have a session ID to use to create a
         * SignalEmitter until a remote device does a joinSession on our
         * hosted session.  This, in turn, means that we have to create the
         * SignalEmitter after we get a sessionJoined() callback in the 
         * SessionPortListener passed into bindSessionPort().  We chose to
         * create the signal emitter for this case in the sessionJoined()
         * callback itself.  Note that this hosted channel signal emitter
         * must be distinct from one constructed for the usual joinSession
         * since a hosted channel may have a remote device do a join at any
         * time, even when we are joined to another session.  If they were
         * not separated, a remote join on the hosted session could redirect
         * messages from the joined session unexpectedly.
         * 
         * So, to summarize, these next few lines handle a relatively complex
         * case.  When we host a chat channel, we do a bindSessionPort which
         * *enables* the creation of a session.  As soon as a remote device
         * joins the hosted chat channel, a session is actually created, and
         * the SessionPortListener sessionJoined() callback is fired.  At that
         * point, we create a separate SignalEmitter using the hosted session's
         * sessionId that we can use to send chat messages to the channel we
         * are hosting.  As soon as the session comes up, we begin receiving
         * chat messages from the session, so we need to filter them until the
         * user joins the hosted chat channel.  In a separate timeline, the
         * user can decide to join the chat channel she is hosting.  She can
         * do so either before or after the corresponding session has been
         * created as a result of a remote device joining the hosted session. 
         * If she joins the hosted channel before the underlying session is
         * created, her chat messages will be discarded.  If she does so after
         * the underlying session is created, there will be a session emitter
         * waiting to use to send chat messages.  In either case, the signal
         * filter will be turned off in order to listen to remote chat
         * messages.
         */
        /**
         * The following is the code for host to join itself
         */
        if (mHostChannelState != HostChannelState.IDLE) {
        	if (mChatApplication.useGetChannelName().equals(mChatApplication.hostGetChannelName())) {              
             	mUseChannelState = UseChannelState.JOINEDSELF;
              	mChatApplication.useSetChannelState(mUseChannelState);
        		mJoinedToSelf = true;
        		
        		//once we know we joined to ourself, we need to advertise that FileInfo property can be retrieved from me, so:
        		//1.we register the property onto the mBus
        		//2.we request a wellKnownName on the mBus and advertise it
        		doRegisterProperty();                
        		doRequestNameAndAdvertiseProperty();               
        		
                return; //this mark the end of host joining itself
        	}
        }
        
        /**
         * The following is the code for other client to join a remote host
         */
       	/*
    	 * We depend on the user interface and model to work together to provide
    	 * a reasonable name.
    	 */
    	String wellKnownName = NAME_PREFIX_SIGNAL + "." + mChatApplication.useGetChannelName();
        
        /*
         * Since we can act as the host of a channel, we know what the other
         * side is expecting to see.
         */
    	short contactPort = CONTACT_PORT;
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
        
        Status status = mBus.joinSession(wellKnownName, contactPort, sessionId, sessionOpts, new SessionListener() {
            /**
             * This method is called when the last remote participant in the 
             * chat session leaves for some reason and we no longer have anyone
             * to chat with.
             *
             * In the class documentation for the BusListener note that it is a
             * requirement for this method to be multi-thread safe.  This is
             * accomplished by the use of a monitor on the ChatApplication as
             * exemplified by the synchronized attribute of the removeFoundChannel
             * method there.
             */
            public void sessionLost(int sessionId, int reason) {
                Log.i(TAG, "BusListener.sessionLost(sessionId=" + sessionId + ",reason=" + reason + ")");
        		mChatApplication.alljoynError(ChatApplication.Module.USE, "The chat session has been lost");
             	mUseChannelState = UseChannelState.IDLE;
              	mChatApplication.useSetChannelState(mUseChannelState);
            }
        });
        
        if (status == Status.OK) {
            Log.i(TAG, "doJoinSession(): use sessionId is " + mUseSessionId);
        	mUseSessionId = sessionId.value;
        } else {
    		mChatApplication.alljoynError(ChatApplication.Module.USE, "Unable to join chat session: (" + status + ")");
        	return;
        }
        
        SignalEmitter emitter = new SignalEmitter(mChatService, mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
        mChatInterface = emitter.getInterface(ChatInterface.class);
        
        // if we are a client joining a remote host, we can try to findAdvertisedName for the property interface
        wellKnownName = NAME_PREFIX_PROPERTY + "." + mChatApplication.useGetChannelName();
        Log.w(TAG, "try to findAdvertisedName: " + wellKnownName);
        status = mBus.findAdvertisedName(wellKnownName);
        if (status != Status.OK){
        	mChatApplication.alljoynError(ChatApplication.Module.USE, "Unable to start finding advertised names: (" + status + ")");
        	return;
        }       
        
     	mUseChannelState = UseChannelState.JOINEDOTHER;
      	mChatApplication.useSetChannelState(mUseChannelState);
    }
    
    
    
    /**
     * Implementation of the functionality related to joining an existing
     * remote session.
     */
    private void doLeaveSession() {
        Log.i(TAG, "doLeaveSession()");
        if (mJoinedToSelf == false) {
        	mBus.leaveSession(mUseSessionId);
        }
        mUseSessionId = -1;
        mJoinedToSelf = false;
     	mUseChannelState = UseChannelState.IDLE;
      	mChatApplication.useSetChannelState(mUseChannelState);
    }
    
    /**
     * The session identifier of the "use" session that the application
     * uses to talk to remote instances.  Set to -1 if not connectecd.
     */
    int mUseSessionId = -1;
    
    /**
     * Implementation of the functionality related to sending messages out over
     * an existing remote session.  Note that we always send all of the
     * messages on the outbound queue, so there may be instances where this
     * method is called and we find nothing to send depending on the races.
     */
    private void doSendMessages() {
        Log.i(TAG, "doSendMessages()");

        String message;
        while ((message = mChatApplication.getOutboundItem()) != null) {
            Log.i(TAG, "doSendMessages(): sending message \"" + message + "\"");
            /*
             * If we are joined to a remote session, we send the message over
             * the mChatInterface.  If we are implicityly joined to a session
             * we are hosting, we send the message over the mHostChatInterface.
             * The mHostChatInterface may or may not exist since it is created
             * when the sessionJoined() callback is fired in the
             * SessionPortListener, so we have to check for it.
             */
			try {
				if (mJoinedToSelf) {
					if (mHostChatInterface != null) {
						mHostChatInterface.Chat(message);
					}
				} else {
					mChatInterface.Chat(message);
				}
			} catch (BusException ex) {
	    		mChatApplication.alljoynError(ChatApplication.Module.USE, "Bus exception while sending message: (" + ex + ")");
			}
    	}
    }

    
    /**
     * The method related to broadcasting file server info
     */
    private void doBroadcastFileInfo() {
    	Log.i(TAG, "doBroadcastFileInfo()");
    	
    	String IP = Utility.getIPAddress(true);
    	if (IP.isEmpty()){
    		mChatApplication.alljoynError(ChatApplication.Module.USE, "Bus exception while broadcase server: no IP yet!");
    		return;
    	}
    	
    	FileInfo mFileInfo = mChatApplication.getFileInfo();
    	
    	//The following should never happen, since the null check happened before this call,
    	//and it was in UseFragment, when Uri is returned in onActivityResult()
    	if (mFileInfo == null)
		{
			Log.w(TAG, "mFileInfo is null!");
			mChatApplication.alljoynError(ChatApplication.Module.USE, "Unable to broadcast a null FileInfo!");
	    	return;
		}
		
		Log.i(TAG, "Constructed a file info: server: "+ mFileInfo.fileServerIP + " port: "+mFileInfo.fileServerPort + " filename: " + mFileInfo.fileName + " filesize: " + mFileInfo.fileSize);
    	
		try{
			
			/**
			 * Assume only host can call this function, then host already set its file when the file is picked,
			 * the host only need to send signal to others to indicate the update of the file shared,
			 * so emit the signal through the remote "mChatInterface"
			 */
			if (mJoinedToSelf)
			{
				if (mHostChatInterface != null) mHostChatInterface.FileInfo(mFileInfo);
				
			}
			
			/*if (mJoinedToSelf){
				if (mHostChatInterface != null){
					mHostChatInterface.FileInfo(mFileInfo);
				}
				//if no one has joined my hosted session yet, mHostChatInterface == null, and I don't need to send server
				//info to myself, so do nothing for "else"
			}
			else {
				//I joined someone else's session, and I want to share my file as a client (host is somewhere else)
				//shoot the signal through the emitter of the remote mChatInterface
				mChatInterface.FileInfo(mFileInfo);
			}*/
		}
		catch (BusException ex){
			mChatApplication.alljoynError(ChatApplication.Module.USE, "Bus exception while broadcasting file server info!");
		} 	
    	
    }
    
    /**
     * First of all, this method is only supposed to be called by a client who joined a remote host, and only when it try to
     * start transfer the file (Request File Button pressed). At this point, if local FileInfo is still null (it missed
     * the broadcast when host selects the file, i.e. it joined late), then it will try to request a FileInfo update by calling
     * here.
     * 
     * Here we first test if mFileInfoPropertyInterface is null or not, if it's null, host hasn't join its own session,
     * treat it as if host hasn't select any file
     * If not null, we call the mFileInfoPropertyInterface.getFileInfo() to request an update, and
     * there are 2 possible results:
     * 1. The host still hasn't select a file yet, at this point, a FileInfo initialized with 0 will be set into local mChatApplication
     * 2. The host has selected the file, then a valid FileInfo object will be stored into local mChatApplication
     * 
     * Either way, a not null FileInfo object will be stored inside local mChatApplication, it's the UseFragment's responsibility 
     * to test if the local FileInfo object is 0 initialized or not. If so, it knows host hasn't select, it should display a dialog 
     * to user. If so, it's time to connect to host and start to transfer file to local sd card.
     */
    private void doUpdateFileInfo(){
    	Log.i(TAG, "doUpdateFileInfo()");
    	
    	// if this is null, then host hasn't join its own channel, the behavior should be the same as the host hasn't select
    	// the file yet, so we set a 0 initialized FileInfo object into the local mChatApplication
    	if (mFileInfoPropertyInterface == null) {
    		Log.w(TAG, "mFileInfoPropertyInterface is null!");
    		mChatApplication.setFileInfo(new FileInfo());
    	}
    	else {
    	// if mFileInfoPropertyInterface is not null, then we call mFileInfoPropertyInterface.getFileInfo() from the property
    	// interface, and manually pull the FileInfo from the host, then store it into local mChatApplication
    		try {
    			
				mChatApplication.setFileInfo(mFileInfoPropertyInterface.getFileInfo());		
				
			} catch (BusException e) {
				// TODO Auto-generated catch block
				mChatApplication.alljoynError(ChatApplication.Module.USE, "Bus exception while update FileInfo: (" + e + ")");				
			}
    	}
    	
    }

    /**
     * Our chat messages are going to be Bus Signals multicast out onto an 
     * associated session.  In order to send signals, we need to define an
     * AllJoyn bus object that will allow us to instantiate a signal emmiter.
     */
    class ChatService implements ChatInterface, BusObject {
    	/**                                                                                                                          
         * Intentionally empty implementation of Chat method.  Since this
         * method is only used as a signal emitter, it will never be called
         * directly.
	     */
    	public void Chat(String str) throws BusException {                                                                                              
        }     
    	
    	public void FileInfo(FileInfo fi) throws BusException{    		
    	}

    }

    /**
     * The ChatService is the instance of an AllJoyn interface that is exported
     * on the bus and allows us to send signals implementing messages
     */
    private ChatService mChatService = new ChatService();
    
    class FileInfoProperty implements FileInfoInterface, BusObject {

		@Override
		public org.alljoyn.bus.sample.chat.FileInfo getFileInfo() throws BusException {
			
			FileInfo mFileInfo =  mChatApplication.getFileInfo();
			
			if (mFileInfo == null){
				//Me, the host, hasn't choose any file yet
				mFileInfo = new FileInfo(); //a fileinfo filled with 0.0.0.0 and port 0
				return mFileInfo;
			}
			
			return mFileInfo;
		}    	
    }
    
    private FileInfoProperty mFileInfoProperty = new FileInfoProperty();

    /**
     * The signal handler for messages received from the AllJoyn bus.
     * 
     * Since the messages sent on a chat channel will be sent using a bus
     * signal, we need to provide a signal handler to receive those signals.
     * This is it.  Note that the name of the signal handler has the first
     * letter capitalized to conform with the DBus convention for signal 
     * handler names.
     */
    @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "Chat")
    public void Chat(String string) {
    	
        /*
    	 * See the long comment in doJoinSession() for more explanation of
    	 * why this is needed.
    	 * 
    	 * The only time we allow a signal from the hosted session ID to pass
    	 * through is if we are in mJoinedToSelf state.  If the source of the
    	 * signal is us, we also filter out the signal since we are going to
    	 * locally echo the signal.

     	 */
    	String uniqueName = mBus.getUniqueName();
    	MessageContext ctx = mBus.getMessageContext();
        Log.i(TAG, "Chat(): use sessionId is " + mUseSessionId);
        Log.i(TAG, "Chat(): message sessionId is " + ctx.sessionId);
        
        /*
         * Always drop our own signals which may be echoed back from the system.
         */
        if (ctx.sender.equals(uniqueName)) {
            Log.i(TAG, "Chat(): dropped our own signal received on session " + ctx.sessionId);
    		return;
    	}

        /*
         * Drop signals on the hosted session unless we are joined-to-self.
         */
        if (mJoinedToSelf == false && ctx.sessionId == mHostSessionId) {
            Log.i(TAG, "Chat(): dropped signal received on hosted session " + ctx.sessionId + " when not joined-to-self");
    		return;
    	}
    	
        /*
         * To keep the application simple, we didn't force users to choose a
         * nickname.  We want to identify the message source somehow, so we
         * just use the unique name of the sender's bus attachment.
         */
        String nickname = ctx.sender;
        nickname = nickname.substring(nickname.length()-10, nickname.length());
        
        Log.i(TAG, "Chat(): signal " + string + " received from nickname " + nickname);
        mChatApplication.newRemoteUserMessage(nickname, string);
    }
    
    @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "FileInfo")
    public void FileInfo(FileInfo  fi) {
    	
    	//when the host requested an update of own fileInfo, we have to update it through mChatApplication
    	String uniqueName = mBus.getUniqueName();
    	MessageContext ctx = mBus.getMessageContext();
    	Log.i(TAG, "FileInfo(): use sessionId is " + mUseSessionId);
    	Log.i(TAG, "FileInfo(): message sessionId is " + ctx.sessionId);
    	
    	if (ctx.sender.equals(uniqueName)){
    		Log.i(TAG, "FileInfo(): dropped our own signal received on session " + ctx.sessionId);
    		return;
    	}
    	
    	if (mJoinedToSelf == false && ctx.sessionId == mHostSessionId) {
    		Log.i(TAG, "FileInfo(): dropped signal recevied on hosted session " + ctx.sessionId + " when not joined-to-self");
    		return;
    	}
    	
    	String nickname = "Xianan";
    	
    	Log.i(TAG, "FileInfo(): signal: server: " + fi.fileServerIP + " server port: " + fi.fileServerPort + " fileName: " + fi.fileName + " fileSize: " + fi.fileSize);
    	String fileInfo = fi.fileServerIP + " " + fi.fileServerPort + " " + fi.fileName + " " + fi.fileSize;
    	
    	mChatApplication.setFileInfo(fi);//update our local fileInfo in the mChatApplication to the new one
    	
    	mChatApplication.newRemoteUserMessage(nickname, fileInfo);    
    
    }
    
    /*
     * Load the native alljoyn_java library.  The actual AllJoyn code is
     * written in C++ and the alljoyn_java library provides the language
     * bindings from Java to C++ and vice versa.
     */
    static {
        Log.i(TAG, "System.loadLibrary(\"alljoyn_java\")");
        System.loadLibrary("alljoyn_java");
    }
}
