/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.inhuasoft.shsclient.Screens;


import com.inhuasoft.shsclient.Engine;
import com.inhuasoft.shsclient.Main;
import com.inhuasoft.shsclient.R;
import com.inhuasoft.shsclient.Services.IScreenService;
import com.inhuasoft.shsclient.widgets.MenubarView;

import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnSipStack;
import org.doubango.ngn.sip.NgnSipSession.ConnectionState;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.ngn.utils.NgnUriUtils;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ScreenHome extends BaseScreen  implements OnClickListener{
	private static String TAG = ScreenHome.class.getCanonicalName();
	
	public static final int HOME_INTENT_FLAG = 0;
	public static final int VIDEO_INTENT_FLAG = HOME_INTENT_FLAG + 1 ;
	public static final int SWITCH_INTENT_FLAG = VIDEO_INTENT_FLAG + 1;
	public static final int CONTROL_INTENT_FLAG = SWITCH_INTENT_FLAG + 1;
	public static final int MORE_INTENT_FLAG = CONTROL_INTENT_FLAG + 1;
	public static final int DIAL_INTENT_FLAG = MORE_INTENT_FLAG + 1;
	public static final int VDIAL_INTENT_FLAG = DIAL_INTENT_FLAG + 1;
	public static final int TWOWAY_INTENT_FLAG = VDIAL_INTENT_FLAG + 1;
	public static final int PHOTO_INTENT_FLAG = TWOWAY_INTENT_FLAG + 1;
	public static final int RECORD_INTENT_FLAG = PHOTO_INTENT_FLAG + 1;
	
	private MenubarView mHomeLayout;
	private MenubarView mVideoLayout;
	private MenubarView mSwitchLayout;
	private MenubarView mControlLayout;
	private MenubarView mMoreLayout;
//	private ImageView mVideoImageView;
//	private ImageView mHomeImageView;
//	private TextView mVideoTextView;
//	private TextView mHomeTextView;

	private FragmentManager mFragmentManager;

	private HomeFragment mHomeFragment;
	private VideoFragment mVideoFragment;
	private SwitchFragment mSwitchFragment;
	private ControlFragment mControlFragment;
	private TwowayVideoFragment mTwowayVideoFragment;
	
	
	
	private static final int MENU_EXIT = 0;
	private static final int MENU_SETTINGS = 1;
	
	
	private final INgnSipService mSipService;
	public final INgnConfigurationService mConfigurationService;
	
	private BroadcastReceiver mSipBroadCastRecv;
	
	
	
	public NgnAVSession mAVSession;
	public boolean mIsVideoCall;
	
	public ScreenHome() {
		super(SCREEN_TYPE.HOME_T, TAG);
		
		mSipService = getEngine().getSipService();
		mConfigurationService = getEngine().getConfigurationService();
	}
	
    public String sessionid ;
   
	class SipLoginThread extends Thread {

		public void run()
		{
			//zwzhu add 
			if(mSipService.getRegistrationState() == ConnectionState.CONNECTING || mSipService.getRegistrationState() == ConnectionState.TERMINATING){
				mSipService.stopStack();
			}
			else if (!mSipService.isRegistered()) {
				mSipService.register(ScreenHome.this);
			}
		}
	}

	private void initViews() {
		// TODO Auto-generated method stub
		mHomeLayout = (MenubarView) findViewById(R.id.home_layout);
		mHomeLayout.setOnClickListener(this);
		mVideoLayout = (MenubarView) findViewById(R.id.video_layout);
		mVideoLayout.setOnClickListener(this);
		mSwitchLayout = (MenubarView) findViewById(R.id.switch_layout);
		mSwitchLayout.setOnClickListener(this);
		mControlLayout = (MenubarView) findViewById(R.id.control_layout);
		mControlLayout.setOnClickListener(this);
		mMoreLayout = (MenubarView) findViewById(R.id.more_layout);
		mMoreLayout.setOnClickListener(this);
		
//		mVideoImageView = (ImageView) findViewById(R.id.video_image);
//		mVideoTextView = (TextView) findViewById(R.id.video_text);
		
//		mHomeImageView = (ImageView) findViewById(R.id.home_image);
//		mHomeTextView = (TextView) findViewById(R.id.home_text);

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		initViews();
        mFragmentManager = getFragmentManager();
		setTabSelection(HOME_INTENT_FLAG);
		
		SipLoginThread sip_login_thread = new SipLoginThread();
		sip_login_thread.start();
		
		
		super.mId = getIntent().getStringExtra("id");
		if(!NgnStringUtils.isNullOrEmpty(super.mId)){
			mAVSession =  NgnAVSession.getSession(NgnStringUtils.parseLong(super.mId, -1));
			if(mAVSession !=  null)
			{
				mAVSession.incRef();
				mAVSession.setContext(this);
				setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
				setTabSelection(TWOWAY_INTENT_FLAG);
				mIsVideoCall = mAVSession.getMediaType() == NgnMediaType.AudioVideo || mAVSession.getMediaType() == NgnMediaType.Video;
			}
		}
		
		
		mSipBroadCastRecv = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
				
				// Registration Event
				if(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT.equals(action)){
					NgnRegistrationEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
					if(args == null){
						Log.e(TAG, "Invalid event args");
						return;
					}
					switch(args.getEventType()){
						case REGISTRATION_NOK:
						case UNREGISTRATION_OK:
						case REGISTRATION_OK:
						case REGISTRATION_INPROGRESS:
						case UNREGISTRATION_INPROGRESS:
						case UNREGISTRATION_NOK:
						default:
						//	((ScreenHomeAdapter)mGridView.getAdapter()).refresh();
							break;
					}
				}
			}
		};
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);
	    registerReceiver(mSipBroadCastRecv, intentFilter);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.home_layout:
			setTabSelection(HOME_INTENT_FLAG);
			break;
		case R.id.video_layout:
			setTabSelection(VIDEO_INTENT_FLAG);
			break;
		case R.id.switch_layout:
			setTabSelection(SWITCH_INTENT_FLAG);
			break;
		case R.id.control_layout:
			setTabSelection(CONTROL_INTENT_FLAG);
			break;
		case R.id.more_layout:

			break;

		default:
			break;
		}
	}
	
	
	
	public void setTabSelection(int index) {
		// TODO Auto-generated method stub
		// 每次选中之前先清楚掉上次的选中状态
		// 开启一个Fragment事务
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
		hideFragments(transaction);
		
		switch (index) {
		case HOME_INTENT_FLAG:
			clearSelection();
//			mHomeLayout.setBackgroundResource(R.drawable.ic_home_bottom_bar_bg);
//			mHomeImageView.setSelected(true);
//			mHomeTextView.setTextColor(getTextColor(R.color.orange));
			mHomeLayout.setSelected(true);
			if(mHomeFragment == null) {
				mHomeFragment = new HomeFragment();
				transaction.add(R.id.main_content, mHomeFragment);
			} else {
				transaction.show(mHomeFragment);
			}
			break;
		case VIDEO_INTENT_FLAG:
			clearSelection();
//			mVideoLayout.setBackgroundResource(R.drawable.ic_home_bottom_bar_bg);
//			mVideoImageView.setSelected(true);
//			mVideoTextView.setTextColor(getTextColor(R.color.orange));
			mVideoLayout.setSelected(true);
			if(mVideoFragment == null) {
				mVideoFragment = new VideoFragment();
				transaction.add(R.id.main_content, mVideoFragment);
			} else {
				transaction.show(mVideoFragment);
			}
			break;
		case SWITCH_INTENT_FLAG:
			clearSelection();
			mSwitchLayout.setSelected(true);
			if(mSwitchFragment == null) {
				mSwitchFragment = new SwitchFragment();
				transaction.add(R.id.main_content, mSwitchFragment);
			} else {
				transaction.show(mSwitchFragment);
			}
			break;
			
		case CONTROL_INTENT_FLAG:
			clearSelection();
			mControlLayout.setSelected(true);
			if(mControlFragment == null) {
				mControlFragment = new ControlFragment();
				transaction.add(R.id.main_content, mControlFragment);
			} else {
				transaction.show(mControlFragment);
			}
			break;
			
		case TWOWAY_INTENT_FLAG:
			if(mTwowayVideoFragment == null) {
				mTwowayVideoFragment = new TwowayVideoFragment();
				transaction.add(R.id.main_content, mTwowayVideoFragment);
			} else {
				transaction.show(mTwowayVideoFragment);
			}
			break;

		default:
			break;
		}
		transaction.commit();
	}

	
	
	@Override
	protected void onDestroy() {
       if(mSipBroadCastRecv != null){
    	   unregisterReceiver(mSipBroadCastRecv);
    	   mSipBroadCastRecv = null;
       }
        
       super.onDestroy();
	}
	
	@Override
	public boolean hasMenu() {
		return true;
	}
	
	@Override
	public boolean createOptionsMenu(Menu menu) {
		menu.add(0, ScreenHome.MENU_SETTINGS, 0, "Settings");
		/*MenuItem itemExit =*/ menu.add(0, ScreenHome.MENU_EXIT, 0, "Exit");
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case ScreenHome.MENU_EXIT:
				((Main)getEngine().getMainActivity()).exit();
				break;
			case ScreenHome.MENU_SETTINGS:
				mScreenService.show(ScreenSettings.class);
				break;
		}
		return true;
	}
	
	
	
	private int getTextColor(int id) {
		// TODO Auto-generated method stub
		return getResources().getColor(id);
	}

	private void clearSelection() {
		// TODO Auto-generated method stub
//		mHomeLayout.setBackgroundDrawable(null);
//		mHomeImageView.setSelected(false);
//		mVideoImageView.setSelected(false);
//		mHomeTextView.setTextColor(getResources().getColor(R.color.black));
//		mVideoTextView.setTextColor(getResources().getColor(R.color.black));
//		mVideoLayout.setBackgroundDrawable(null);
//		mSwitchLayout.setBackgroundDrawable(null);
//		mControlLayout.setBackgroundDrawable(null);
		mHomeLayout.setSelected(false);
		mVideoLayout.setSelected(false);
		mSwitchLayout.setSelected(false);
		mControlLayout.setSelected(false);
		mMoreLayout.setSelected(false);
	}

	/**
	 * 将所有的Fragment都置为隐藏状态。
	 * 
	 * @param transaction
	 *            用于对Fragment执行操作的事务
	 */
	private void hideFragments(FragmentTransaction transaction) {
		if (mVideoFragment != null) {
			transaction.hide(mVideoFragment);
		}
		if (mHomeFragment != null) {
			transaction.hide(mHomeFragment);
		}
		if (mSwitchFragment != null) {
			transaction.hide(mSwitchFragment);
		}
		if (mControlFragment != null) {
			transaction.hide(mControlFragment);
		}
		if(mTwowayVideoFragment != null) {
			transaction.hide(mTwowayVideoFragment);
		}
	}
	
	
	public static boolean receiveCall(NgnAVSession avSession){
		((Engine)Engine.getInstance()).getScreenService().bringToFront(Main.ACTION_SHOW_AVSCREEN,
				new String[] {"session-id", Long.toString(avSession.getId())}
		);
		return true;
	}

	
	
	public static boolean makeCall(String remoteUri, NgnMediaType mediaType){
		final Engine engine = (Engine)Engine.getInstance();
		final INgnSipService sipService = engine.getSipService();
		final INgnConfigurationService configurationService = engine.getConfigurationService();
		final IScreenService screenService = engine.getScreenService();
		final String validUri = NgnUriUtils.makeValidSipUri(remoteUri);
		if(validUri == null){
			Log.e(TAG, "failed to normalize sip uri '" + remoteUri + "'");
			return false;
		}
		else{
			remoteUri = validUri;
			if(remoteUri.startsWith("tel:")){
				// E.164 number => use ENUM protocol
				final NgnSipStack sipStack = sipService.getSipStack();
				if(sipStack != null){
					String phoneNumber = NgnUriUtils.getValidPhoneNumber(remoteUri);
					if(phoneNumber != null){
						String enumDomain = configurationService.getString(
								NgnConfigurationEntry.GENERAL_ENUM_DOMAIN, NgnConfigurationEntry.DEFAULT_GENERAL_ENUM_DOMAIN);
						String sipUri = sipStack.dnsENUM("E2U+SIP", phoneNumber, enumDomain);
						if(sipUri != null){
							remoteUri = sipUri;
						}
					}
				}
			}
		}
		
		final NgnAVSession avSession = NgnAVSession.createOutgoingSession(sipService.getSipStack(), mediaType);
		avSession.setRemotePartyUri(remoteUri); // HACK
		screenService.show(ScreenHome.class, Long.toString(avSession.getId()));
		
		
		// Hold the active call
		final NgnAVSession activeCall = NgnAVSession.getFirstActiveCallAndNot(avSession.getId());
		if(activeCall != null){
			activeCall.holdCall();
		}
		
		return avSession.makeCall(remoteUri);
	}
	
	
}
