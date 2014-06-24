package com.inhuasoft.shsclient.Screens;

import java.util.Date;
import java.util.TimerTask;

import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.events.NgnInviteEventTypes;
import org.doubango.ngn.events.NgnMediaPluginEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.model.NgnContact;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession.InviteState;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnGraphicsUtils;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.ngn.utils.NgnTimer;
import org.doubango.ngn.utils.NgnUriUtils;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inhuasoft.shsclient.CustomDialog;
import com.inhuasoft.shsclient.IMSDroid;
import com.inhuasoft.shsclient.R;


public class TwowayVideoFragment extends Fragment {
	
	private static final String TAG = TwowayVideoFragment.class.getCanonicalName();
	private static int mCountBlankPacket;
	private static int mLastRotation; // values: degrees
	private boolean mSendDeviceInfo;
	private int mLastOrientation; // values: portrait, landscape...
	private String mRemotePartyDisplayName;
	private Bitmap mRemotePartyPhoto;
	
	private ViewType mCurrentView;
	private LayoutInflater mInflater;
	private LinearLayout mMainLayout;
	private BroadcastReceiver mBroadCastRecv;
	
	
	private View mViewTrying;
	private View mViewInAudioCall;
	private View mViewInCallVideo;
	private LinearLayout mViewLocalVideoPreview;
	private FrameLayout mViewRemoteVideoPreview;
	private View mViewTermwait;
	private View mViewProxSensor;
	
	private final NgnTimer mTimerInCall;
	private final NgnTimer mTimerSuicide;
	private final NgnTimer mTimerBlankPacket;
	
	
	private TextView mTvInfo;
	private TextView mTvDuration;
	
	private AlertDialog mTransferDialog;
	private NgnAVSession mAVTransfSession;
	
	private KeyguardLock mKeyguardLock;
	private OrientationEventListener mListener;
	
	private PowerManager.WakeLock mWakeLock;
	
	private static final int SELECT_CONTENT = 1;
	
	private final static int MENU_PICKUP = 0;
	private final static int MENU_HANGUP= 1;
	private final static int MENU_HOLD_RESUME = 2;
	private final static int MENU_SEND_STOP_VIDEO = 3;
	private final static int MENU_SHARE_CONTENT = 4;
	private final static int MENU_SPEAKER = 5;
	
	private static boolean SHOW_SIP_PHRASE = true;
	
	ImageButton imgbtn_hangup;
	
	private static enum ViewType{
		ViewNone,
		ViewTrying,
		ViewInCall,
		ViewProxSensor,
		ViewTermwait
	}
	
	public TwowayVideoFragment()
	{
	    mCurrentView = ViewType.ViewNone;
		mTimerInCall = new NgnTimer();
		mTimerSuicide = new NgnTimer();
		mTimerBlankPacket = new NgnTimer();
	}
	
	
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		mCountBlankPacket = 0;
		mLastRotation = -1;
		mLastOrientation = -1;
		mInflater = LayoutInflater.from(getActivity());
		
		mBroadCastRecv = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(intent.getAction())){
					handleSipEvent(intent);
				}
				else if(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT.equals(intent.getAction())){
					handleMediaEvent(intent);
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
		intentFilter.addAction(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT);
	    getActivity().registerReceiver(mBroadCastRecv, intentFilter);
	    
			
		mMainLayout = (LinearLayout)getActivity().findViewById(R.id.linearLayout_phone_main);
        loadView();
	}




	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_twoway_video, container, false);
    }


			@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
				
	   if(mBroadCastRecv != null){
	       getActivity().unregisterReceiver(mBroadCastRecv);
	       mBroadCastRecv = null;
	     }
			       
		  mTimerInCall.cancel();
	      mTimerSuicide.cancel();
	      cancelBlankPacket();
			       
	      if(mWakeLock != null && mWakeLock.isHeld()){
			 mWakeLock.release();
		  }
		 mWakeLock = null;
			       
	     if(((ScreenHome)getActivity()).mAVSession != null){
	    	((ScreenHome)getActivity()).mAVSession.setContext(null);
	    	((ScreenHome)getActivity()).mAVSession.decRef();
	     }		
	     super.onDestroy();
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		

		
		if(mWakeLock != null && mWakeLock.isHeld()){
			mWakeLock.release();
		}
		
		if (mListener != null && mListener.canDetectOrientation()) {
			mListener.disable();
		}
		
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		

		
		if(((ScreenHome)getActivity()).mAVSession != null){
			if (((ScreenHome)getActivity()).mAVSession.getState() == InviteState.INCALL) {
				mTimerInCall.schedule(mTimerTaskInCall, 0, 1000);
			}
		}

		if (mListener != null && mListener.canDetectOrientation()) {
			mListener.enable();
		}
		
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		
		final KeyguardManager keyguardManager = IMSDroid.getKeyguardManager();
		if(keyguardManager != null){
			if(mKeyguardLock == null){
				mKeyguardLock = keyguardManager.newKeyguardLock(TwowayVideoFragment.TAG);
			}
			if(keyguardManager.inKeyguardRestrictedInputMode()){
				mKeyguardLock.disableKeyguard();
			}
		}
		
		final PowerManager powerManager = IMSDroid.getPowerManager();
		if(powerManager != null && mWakeLock == null){
			mWakeLock = powerManager.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
			if(mWakeLock != null){
				mWakeLock.acquire();
			}
		}
		
	}
  
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
		if(mKeyguardLock != null){
			mKeyguardLock.reenableKeyguard();
		}
		
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		if(((ScreenHome)getActivity()).mAVSession == null){
			return ;
		}
		
		MenuItem itemSendStopVideo = null;
		
		MenuItem itemPickUp = menu.add(0, TwowayVideoFragment.MENU_PICKUP, 0, getString(R.string.string_answer)).setIcon(R.drawable.phone_pick_up_48);
		MenuItem itemHangUp = menu.add(0, TwowayVideoFragment.MENU_HANGUP, 0, getString(R.string.string_endcall)).setIcon(R.drawable.phone_hang_up_48);
		MenuItem itemHoldResume = menu.add(0, TwowayVideoFragment.MENU_HOLD_RESUME, 0, ((ScreenHome)getActivity()).mAVSession.isLocalHeld() ? getString(R.string.string_resume) : getString(R.string.string_hold))
			.setIcon(((ScreenHome)getActivity()).mAVSession.isLocalHeld() ? R.drawable.phone_resume_48 : R.drawable.phone_hold_48);
		if(((ScreenHome)getActivity()).mIsVideoCall){
			itemSendStopVideo = menu.add(1, TwowayVideoFragment.MENU_SEND_STOP_VIDEO, 0, getString(R.string.string_send_video));
		}
	//	MenuItem itemShareContent = menu.add(1, ScreenAV.MENU_SHARE_CONTENT, 0, "Share Content").setIcon(R.drawable.image_gallery_48);
		MenuItem itemSpeaker = menu.add(1, TwowayVideoFragment.MENU_SPEAKER, 0, ((ScreenHome)getActivity()).mAVSession.isSpeakerOn() ? getString(R.string.string_speaker_off) : getString(R.string.string_speaker_on))
			.setIcon(R.drawable.phone_speaker_48);
		
		switch(((ScreenHome)getActivity()).mAVSession.getState()){
			case INCOMING:
			{
				itemPickUp.setEnabled(true);
				itemHangUp.setEnabled(true);
				itemHoldResume.setEnabled(false);
				itemSpeaker.setEnabled(false);
				if(itemSendStopVideo != null){
					itemSendStopVideo.setEnabled(false);
				}
			//	itemShareContent.setEnabled(false);
				break;
			}
			
			case INPROGRESS:
			{
				itemPickUp.setEnabled(false);
				itemHangUp.setEnabled(true);
				itemHoldResume.setEnabled(false);
				itemSpeaker.setEnabled(false);
				if(itemSendStopVideo != null){
					itemSendStopVideo.setEnabled(false);
				}
			//	itemShareContent.setEnabled(false);
				break;
			}
			
			case INCALL:
			{
				itemHangUp.setEnabled(true);
				itemHoldResume.setEnabled(true);
				itemSpeaker.setEnabled(true);
				
				if(itemSendStopVideo != null){
					itemSendStopVideo.setTitle(((ScreenHome)getActivity()).mAVSession.isSendingVideo() ? "Stop Video" : getString(R.string.string_send_video))
					.setIcon(((ScreenHome)getActivity()).mAVSession.isSendingVideo() ? R.drawable.video_stop_48 : R.drawable.video_start_48);
					itemSendStopVideo.setEnabled(true);
					//Replace Answer by camera switcher
					itemPickUp.setEnabled(true);
					itemPickUp.setTitle(getString(R.string.string_switch_camera)).setIcon(R.drawable.refresh_48);
				}
				else{
					itemPickUp.setEnabled(false);
				}
			//	itemShareContent.setEnabled(true);
				break;
			}
				
			case TERMINATED:
			case TERMINATING:
			{
				itemPickUp.setEnabled(false);
				itemHangUp.setEnabled(false);
				itemHoldResume.setEnabled(false);
				itemSpeaker.setEnabled(false);
				if(itemSendStopVideo != null){
					itemSendStopVideo.setEnabled(false);
				}
			//	itemShareContent.setEnabled(false);
				break;
			}
			default:
			{
				break;
			}
		}
		
		return ;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(((ScreenHome)getActivity()).mAVSession == null){
			return true;
		}
		switch(item.getItemId()){
			case TwowayVideoFragment.MENU_PICKUP:
			{
				if (((ScreenHome)getActivity()).mAVSession.getState() == InviteState.INCALL) {
					//Log.d(TAG, "Toggle Camera");
					((ScreenHome)getActivity()).mAVSession.toggleCamera();
				} else {
					acceptCall();
				}
				break;
			}
				
			case TwowayVideoFragment.MENU_HANGUP:
			{
				if(mTvInfo != null){
					mTvInfo.setText("Ending the call...");
				}
				hangUpCall();
				break;
			}
				
			case TwowayVideoFragment.MENU_HOLD_RESUME:
			{
				if(((ScreenHome)getActivity()).mAVSession.isLocalHeld()){
					((ScreenHome)getActivity()).mAVSession.resumeCall();
				}
				else{
					((ScreenHome)getActivity()).mAVSession.holdCall();
				}
				break;
			}
				
			case TwowayVideoFragment.MENU_SEND_STOP_VIDEO:
			{
				 startStopVideo(!((ScreenHome)getActivity()).mAVSession.isSendingVideo());
				 break;
			}
				
			case TwowayVideoFragment.MENU_SHARE_CONTENT:
			{
				 Intent intent = new Intent();
				 intent.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).setAction(Intent.ACTION_GET_CONTENT);
				 startActivityForResult(Intent.createChooser(intent, "Select content"), SELECT_CONTENT);
				break;
			}
			
			case TwowayVideoFragment.MENU_SPEAKER:
			{
				((ScreenHome)getActivity()).mAVSession.toggleSpeakerphone();
				break;
			}
		}
		return true;
	}

	  private boolean hangUpCall(){
		if(((ScreenHome)getActivity()).mAVSession != null){
			return ((ScreenHome)getActivity()).mAVSession.hangUpCall();
		}
 		return false;
	  }
	
	  private boolean acceptCall(){
		if(((ScreenHome)getActivity()).mAVSession != null){
			return ((ScreenHome)getActivity()).mAVSession.acceptCall();
		}
		return false;
	  }
	
	
	  
	  private void handleMediaEvent(Intent intent){
			final String action = intent.getAction();
		
			if(NgnMediaPluginEventArgs.ACTION_MEDIA_PLUGIN_EVENT.equals(action)){
				NgnMediaPluginEventArgs args = intent.getParcelableExtra(NgnMediaPluginEventArgs.EXTRA_EMBEDDED);
				if(args == null){
				//	Log.e(TAG, "Invalid event args");
					return;
				}
				
				switch(args.getEventType()){
					case STARTED_OK: //started or restarted (e.g. reINVITE)
					{
						((ScreenHome)getActivity()).mIsVideoCall = (((ScreenHome)getActivity()).mAVSession.getMediaType() == NgnMediaType.AudioVideo || ((ScreenHome)getActivity()).mAVSession.getMediaType() == NgnMediaType.Video);
						loadView();
						
						break;
					}
					case PREPARED_OK:
					case PREPARED_NOK:
					case STARTED_NOK:
					case STOPPED_OK:
					case STOPPED_NOK:
					case PAUSED_OK:
					case PAUSED_NOK:
					{
						break;
					}
				}
			}
		}
		
		private void handleSipEvent(Intent intent){
			@SuppressWarnings("unused")
			InviteState state;
			if(((ScreenHome)getActivity()).mAVSession == null){
				//Log.e(TAG, "Invalid session object");
				return;
			}
			final String action = intent.getAction();
			if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)){
				NgnInviteEventArgs args = intent.getParcelableExtra(NgnInviteEventArgs.EXTRA_EMBEDDED);
				if(args == null){
					//Log.e(TAG, "Invalid event args");
					return;
				}
				if(args.getSessionId() != ((ScreenHome)getActivity()).mAVSession.getId()){
					if(args.getEventType() == NgnInviteEventTypes.REMOTE_TRANSFER_INPROGESS){
						// Native code created new session handle to be used to replace the current one (event = "tsip_i_ect_newcall").
						mAVTransfSession = NgnAVSession.getSession(args.getSessionId());
					}
					return;
				}
				
				switch((state = ((ScreenHome)getActivity()).mAVSession.getState())){
					case NONE:
					default:
						break;
						
					case INCOMING:
					case INPROGRESS:
					case REMOTE_RINGING:
						loadTryingView();
						break;
						
					case EARLY_MEDIA:
					case INCALL:
						//if(state == InviteState.INCALL){
							// stop using the speaker (also done in ServiceManager())
							((ScreenHome)getActivity()).getEngine().getSoundService().stopRingTone();
							((ScreenHome)getActivity()).mAVSession.setSpeakerphoneOn(false);
						//}
						//if(state == InviteState.INCALL){
							loadInCallView();
						//}
						// Send blank packets to open NAT pinhole
						if(((ScreenHome)getActivity()).mAVSession != null){
							//applyCamRotation(mAVSession.compensCamRotation(true));
							mTimerBlankPacket.schedule(mTimerTaskBlankPacket, 0, 250);
							if(!((ScreenHome)getActivity()).mIsVideoCall){
								mTimerInCall.schedule(mTimerTaskInCall, 0, 1000);
							}
						}
						
						// release power lock if not video call
						if(!((ScreenHome)getActivity()).mIsVideoCall && mWakeLock != null && mWakeLock.isHeld()){
							mWakeLock.release();
				        }
						
						switch(args.getEventType()){
							case REMOTE_DEVICE_INFO_CHANGED:
								{
								//	Log.d(TAG, String.format("Remote device info changed: orientation: %s", mAVSession.getRemoteDeviceInfo().getOrientation()));
									break;
								}
							case MEDIA_UPDATED:
								{
									if((((ScreenHome)getActivity()).mIsVideoCall = (((ScreenHome)getActivity()).mAVSession.getMediaType() == NgnMediaType.AudioVideo || ((ScreenHome)getActivity()).mAVSession.getMediaType() == NgnMediaType.Video))){
										loadInCallVideoView();
									}
									else{
										loadInCallAudioView();
									}
									break;
								}
							case LOCAL_TRANSFER_TRYING:
			                    {
			                    	if (mTvInfo != null) {
			                    		mTvInfo.setText("Call Transfer: Initiated");
			                    	}
			                        break;
			                    }
			                case LOCAL_TRANSFER_FAILED:
			                    {
			                    	if (mTvInfo != null) {
			                    		mTvInfo.setText("Call Transfer: Failed");
			                    	}
			                        break;
			                    }
			                case LOCAL_TRANSFER_ACCEPTED:
			                    {
			                    	if (mTvInfo != null) {
			                    		mTvInfo.setText("Call Transfer: Accepted");
			                    	}
			                        break;
			                    }
			                case LOCAL_TRANSFER_COMPLETED:
			                    {
			                    	if (mTvInfo != null) {
			                    		mTvInfo.setText("Call Transfer: Completed");
			                    	}
			                        break;
			                    }
			                case LOCAL_TRANSFER_NOTIFY:
			                case REMOTE_TRANSFER_NOTIFY:
			                    {
			                    	if (mTvInfo != null && ((ScreenHome)getActivity()).mAVSession != null) {
			                    		short sipCode = intent.getShortExtra(NgnInviteEventArgs.EXTRA_SIPCODE, (short)0);
			                    		
			                    		mTvInfo.setText("Call Transfer: " + sipCode + " " + args.getPhrase());
			                    		if (sipCode >= 300 && ((ScreenHome)getActivity()).mAVSession.isLocalHeld()){
			                    			((ScreenHome)getActivity()).mAVSession.resumeCall();
			                            }
			                    	}
			                        break;
			                    }
		
			                case REMOTE_TRANSFER_REQUESTED:
			                    {
			                    	String referToUri = intent.getStringExtra(NgnInviteEventArgs.EXTRA_REFERTO_URI);
			                    	if (!NgnStringUtils.isNullOrEmpty(referToUri)) {
			                    		String referToName = NgnUriUtils.getDisplayName(referToUri);
			                    		if (!NgnStringUtils.isNullOrEmpty(referToName)) {
			                    			mTransferDialog = CustomDialog.create(
			                    					((ScreenHome)getActivity()),
			        								R.drawable.exit_48,
			        								null,
			        								"Call Transfer to " + referToName + " requested. Do you accept?",
			        								"Yes",
			        								new DialogInterface.OnClickListener() {
			        									@Override
			        									public void onClick(DialogInterface dialog, int which) {
			        										dialog.cancel();
			        										mTransferDialog = null;
			        										if (((ScreenHome)getActivity()).mAVSession != null) {
			        											((ScreenHome)getActivity()).mAVSession.acceptCallTransfer();
			        										}
			        									}
			        								}, "No",
			        								new DialogInterface.OnClickListener() {
			        									@Override
			        									public void onClick(DialogInterface dialog, int which) {
			        										dialog.cancel();
			        										mTransferDialog = null;
			        										if (((ScreenHome)getActivity()).mAVSession != null) {
			        											((ScreenHome)getActivity()).mAVSession.rejectCallTransfer();
			        										}
			        									}
			        								});
			                    			mTransferDialog.show();
			                    		}
			                    	}
			                        break;
			                    }
			               
			                case REMOTE_TRANSFER_FAILED:
			                    {
			                    	if (mTransferDialog != null) {
			                    		mTransferDialog.cancel();
			                    		mTransferDialog = null;
			                    	}
			                    	mAVTransfSession = null;
			                        break;
			                    }
			                case REMOTE_TRANSFER_COMPLETED:
			                    {
			                    	if (mTransferDialog != null) {
			                    		mTransferDialog.cancel();
			                    		mTransferDialog = null;
			                    	}
			                        if (mAVTransfSession != null)
			                        {
			                        	mAVTransfSession.setContext(((ScreenHome)getActivity()).mAVSession.getContext());
			                        	((ScreenHome)getActivity()).mAVSession = mAVTransfSession;
			                            mAVTransfSession = null;
			                            loadInCallView(true);
			                        }
			                        break;
			                    }
							default:
								{
									break;
								}
						}					
						break;
						
					case TERMINATING:
					case TERMINATED:
						if (mTransferDialog != null) {
	                		mTransferDialog.cancel();
	                		mTransferDialog = null;
	                	}
						mTimerSuicide.schedule(mTimerTaskSuicide, new Date(new Date().getTime() + 1500));
						mTimerTaskInCall.cancel();
						mTimerBlankPacket.cancel();
						//loadTermView(SHOW_SIP_PHRASE ? args.getPhrase() : null);
						
						// release power lock
						if(mWakeLock != null && mWakeLock.isHeld()){
							mWakeLock.release();
				        }
						break;
				}
			}
		}
		
		
		private void loadView(){
			switch(((ScreenHome)getActivity()).mAVSession.getState()){
		        case INCOMING:
		        	acceptCall();
					break;
		        case INPROGRESS:
		        case REMOTE_RINGING:
		        	loadTryingView();
		        	break;
		        	
		        case INCALL:
		        case EARLY_MEDIA:
		        	loadInCallView();
		        	break;
		        	
		        case NONE:
		        case TERMINATING:
		        case TERMINATED:
		        default:
		        	loadTermView();
		        	break;
		    }
		}
		
		private void loadTryingView(){
			if(mCurrentView == ViewType.ViewTrying){
				return;
			}
			//Log.d(TAG, "loadTryingView()");	
			
			if(mViewTrying == null){
				mViewTrying = mInflater.inflate(R.layout.view_call_trying, null);
				//loadKeyboard(mViewTrying);
			}
			mTvInfo = (TextView)mViewTrying.findViewById(R.id.view_call_trying_textView_info);
			
			final TextView tvRemote = (TextView)mViewTrying.findViewById(R.id.view_call_trying_textView_remote);
			final ImageButton btPick = (ImageButton)mViewTrying.findViewById(R.id.view_call_trying_imageButton_pick);
			final ImageButton btHang = (ImageButton)mViewTrying.findViewById(R.id.view_call_trying_imageButton_hang);
			final ImageView ivAvatar = (ImageView)mViewTrying.findViewById(R.id.view_call_trying_imageView_avatar);
			btPick.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptCall();
				}
			});
			btHang.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					hangUpCall();
				}
			});
			
			switch(((ScreenHome)getActivity()).mAVSession.getState()){
		        case INCOMING:
		        	mTvInfo.setText(getString(R.string.string_call_incoming));
		        	break;
		        case INPROGRESS:
		        case REMOTE_RINGING:
		        case EARLY_MEDIA:
		        default:
		        	mTvInfo.setText(getString(R.string.string_call_outgoing));
		        	btPick.setVisibility(View.GONE);
		        	break;
		    }
			
			tvRemote.setText(mRemotePartyDisplayName);
			if(mRemotePartyPhoto != null){
				ivAvatar.setImageBitmap(mRemotePartyPhoto);
			}
			
			mMainLayout.removeAllViews();
			mMainLayout.addView(mViewTrying);
			mCurrentView = ViewType.ViewTrying;
		}
		
		private void loadInCallAudioView(){
			//Log.d(TAG, "loadInCallAudioView()");
			if(mViewInAudioCall == null){
				mViewInAudioCall = mInflater.inflate(R.layout.view_call_incall_audio, null);
				//loadKeyboard(mViewInAudioCall);
			}
			mTvInfo = (TextView)mViewInAudioCall.findViewById(R.id.view_call_incall_audio_textView_info);
			
			final TextView tvRemote = (TextView)mViewInAudioCall.findViewById(R.id.view_call_incall_audio_textView_remote);
			final ImageButton btHang = (ImageButton)mViewInAudioCall.findViewById(R.id.view_call_incall_audio_imageButton_hang);
			final ImageView ivAvatar = (ImageView)mViewInAudioCall.findViewById(R.id.view_call_incall_audio_imageView_avatar);
			mTvDuration = (TextView)mViewInAudioCall.findViewById(R.id.view_call_incall_audio_textView_duration);
			
			btHang.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					hangUpCall();
				}
			});
			
			tvRemote.setText(mRemotePartyDisplayName);
			if(mRemotePartyPhoto != null){
				ivAvatar.setImageBitmap(mRemotePartyPhoto);
			}
			mTvInfo.setText(getString(R.string.string_incall));
			
			mViewInAudioCall.findViewById(R.id.view_call_incall_audio_imageView_secure)
				.setVisibility(((ScreenHome)getActivity()).mAVSession.isSecure() ? View.VISIBLE : View.INVISIBLE);
			
			mMainLayout.removeAllViews();
			mMainLayout.addView(mViewInAudioCall);
			mCurrentView = ViewType.ViewInCall;
		}
		
		private void loadInCallVideoView(){
			//Log.d(TAG, "loadInCallVideoView()");
			if(mViewInCallVideo == null){
				mViewInCallVideo = mInflater.inflate(R.layout.view_call_incall_video, null);
				//mViewLocalVideoPreview = (FrameLayout)mViewInCallVideo.findViewById(R.id.view_call_incall_video_FrameLayout_local_video);
				mViewLocalVideoPreview = (LinearLayout)getActivity().findViewById(R.id.linearLayout_phone_local);
				mViewRemoteVideoPreview = (FrameLayout)mViewInCallVideo.findViewById(R.id.view_call_incall_video_FrameLayout_remote_video);
			}
			if(mTvDuration != null){
				synchronized(mTvDuration){
			        mTvDuration = null;
				}
			}
			mTvInfo = null;
			mMainLayout.removeAllViews();
			mMainLayout.addView(mViewInCallVideo);
			
			final View viewSecure = mViewInCallVideo.findViewById(R.id.view_call_incall_video_imageView_secure);
			if(viewSecure != null){
				viewSecure.setVisibility(((ScreenHome)getActivity()).mAVSession.isSecure() ? View.VISIBLE : View.INVISIBLE);
			}
			
			// Video Consumer
			loadVideoPreview();
			
			// Video Producer
			startStopVideo(((ScreenHome)getActivity()).mAVSession.isSendingVideo());
			
			mCurrentView = ViewType.ViewInCall;
		}
		
		private void loadInCallView(boolean force){
			if(mCurrentView == ViewType.ViewInCall && !force){
				return;
			}
			//Log.d(TAG, "loadInCallView()");
			
			if(((ScreenHome)getActivity()).mIsVideoCall){
				loadInCallVideoView();
			}
			else{
				loadInCallAudioView();
			}
		}
		
		private void loadInCallView(){
			loadInCallView(false);
		}
		
		private void loadProxSensorView(){
			if(mCurrentView == ViewType.ViewProxSensor){
				return;
			}
			//Log.d(TAG, "loadProxSensorView()");
			if(mViewProxSensor == null){
				mViewProxSensor = mInflater.inflate(R.layout.view_call_proxsensor, null);
			}
			mMainLayout.removeAllViews();
			mMainLayout.addView(mViewProxSensor);
			mCurrentView = ViewType.ViewProxSensor;
		}
		
		private void loadTermView(String phrase){
			//Log.d(TAG, "loadTermView()");
			
			if(mViewTermwait == null){
				mViewTermwait = mInflater.inflate(R.layout.view_call_trying, null);
				//loadKeyboard(mViewTermwait);
			}
			mTvInfo = (TextView)mViewTermwait.findViewById(R.id.view_call_trying_textView_info);
			mTvInfo.setText(NgnStringUtils.isNullOrEmpty(phrase) ? getString(R.string.string_call_terminated) : phrase);
			
			// loadTermView() could be called twice (onTermwait() and OnTerminated) and this is why we need to
			// update the info text for both
			if(mCurrentView == ViewType.ViewTermwait){
				return;
			}
			
			final TextView tvRemote = (TextView)mViewTermwait.findViewById(R.id.view_call_trying_textView_remote);
			final ImageView ivAvatar = (ImageView)mViewTermwait.findViewById(R.id.view_call_trying_imageView_avatar);
			mViewTermwait.findViewById(R.id.view_call_trying_imageButton_pick).setVisibility(View.GONE);
			mViewTermwait.findViewById(R.id.view_call_trying_imageButton_hang).setVisibility(View.GONE);
			//mViewTermwait.setBackgroundResource(R.drawable.grad_bkg_termwait);
			
			tvRemote.setText(mRemotePartyDisplayName);
			if(mRemotePartyPhoto != null){
				ivAvatar.setImageBitmap(mRemotePartyPhoto);
			}
			
			mMainLayout.removeAllViews();
			mMainLayout.addView(mViewTermwait);
			mCurrentView = ViewType.ViewTermwait;
		}
		
		private void loadTermView(){
			loadTermView(null);
		}
		
		private void loadVideoPreview(){
			mViewRemoteVideoPreview.removeAllViews();
	        final View remotePreview = ((ScreenHome)getActivity()).mAVSession.startVideoConsumerPreview();
			if(remotePreview != null){
	            final ViewParent viewParent = remotePreview.getParent();
	            if(viewParent != null && viewParent instanceof ViewGroup){
	                  ((ViewGroup)(viewParent)).removeView(remotePreview);
	            }
	            mViewRemoteVideoPreview.addView(remotePreview);
	        }
		}
		
		private final TimerTask mTimerTaskInCall = new TimerTask(){
			@Override
			public void run() {
				if(((ScreenHome)getActivity()).mAVSession != null && mTvDuration != null){
					synchronized(mTvDuration){
						final Date date = new Date(new Date().getTime() - ((ScreenHome)getActivity()).mAVSession.getStartTime());
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								try{
								//	mTvDuration.setText(sDurationTimerFormat.format(date));
								}
								catch(Exception e){}
							}});
					}
				}
			}
		};
		
		private final TimerTask mTimerTaskBlankPacket = new TimerTask(){
			@Override
			public void run() {	
				//Log.d(TAG,"Resending Blank Packet " +String.valueOf(mCountBlankPacket));
				if (mCountBlankPacket < 3) {
					if (((ScreenHome)getActivity()).mAVSession != null) {
						((ScreenHome)getActivity()).mAVSession.pushBlankPacket();
					}
					mCountBlankPacket++;
				}
				else {
					cancel();
					mCountBlankPacket=0;
				}
			}
		};
		
		private void cancelBlankPacket(){
			if(mTimerBlankPacket != null){
				mTimerBlankPacket.cancel();
				mCountBlankPacket=0;
			}
		}
		
		private final TimerTask mTimerTaskSuicide = new TimerTask(){
			@Override
			public void run() {
		/*		getActivity().runOnUiThread(new Runnable() {
					public void run() {
						IBaseScreen currentScreen = ((ScreenHome)getActivity()).mScreenService.getCurrentScreen();
						boolean gotoHome = (currentScreen != null && currentScreen.getId() == getId());
						if(gotoHome){
							mScreenService.show(ScreenHome.class);
						}
						mScreenService.destroy(getId());
					}});*/
			}
		};
		
		
		
		private void startStopVideo(boolean bStart){
		//	Log.d(TAG, "startStopVideo("+bStart+")");
			if(!((ScreenHome)getActivity()).mIsVideoCall){
				return;
			}
			
			((ScreenHome)getActivity()).mAVSession.setSendingVideo(bStart);
			
			if(mViewLocalVideoPreview != null){
				mViewLocalVideoPreview.removeAllViews();
				if(bStart){
					cancelBlankPacket();
					final View localPreview = ((ScreenHome)getActivity()).mAVSession.startVideoProducerPreview();
					if(localPreview != null){
						final ViewParent viewParent = localPreview.getParent();
						if(viewParent != null && viewParent instanceof ViewGroup){
							((ViewGroup)(viewParent)).removeView(localPreview);
						}
						if(localPreview instanceof SurfaceView){
							((SurfaceView)localPreview).setZOrderOnTop(true);
						}
						mViewLocalVideoPreview.addView(localPreview);
						mViewLocalVideoPreview.bringChildToFront(localPreview);
					}
				}
				mViewLocalVideoPreview.setVisibility(bStart ? View.VISIBLE : View.GONE);
				mViewLocalVideoPreview.bringToFront();
			}
		}
		
		


	}


