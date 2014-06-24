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


import com.inhuasoft.shsclient.Main;
import com.inhuasoft.shsclient.R;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnSipSession.ConnectionState;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	
	private LinearLayout mHomeLayout;
	private LinearLayout mVideoLayout;
	private LinearLayout mSwitchLayout;
	private LinearLayout mControlLayout;
	private LinearLayout mMoreLayout;
	private ImageView mVideoImageView;
	private ImageView mHomeImageView;
	private TextView mVideoTextView;
	private TextView mHomeTextView;

	private FragmentManager mFragmentManager;

	private HomeFragment mHomeFragment;
	private VideoFragment mVideoFragment;
	private TwowayVideoFragment mTwowayVideoFragment;
	
	
	
	private static final int MENU_EXIT = 0;
	private static final int MENU_SETTINGS = 1;
	
	
	private final INgnSipService mSipService;
	private final INgnConfigurationService mConfigurationService;
	
	private BroadcastReceiver mSipBroadCastRecv;
	
	public ScreenHome() {
		super(SCREEN_TYPE.HOME_T, TAG);
		
		mSipService = getEngine().getSipService();
		mConfigurationService = getEngine().getConfigurationService();
	}
	
	
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
		mHomeLayout = (LinearLayout) findViewById(R.id.home_layout);
		mHomeLayout.setOnClickListener(this);
		mVideoLayout = (LinearLayout) findViewById(R.id.video_layout);
		mVideoLayout.setOnClickListener(this);
		mSwitchLayout = (LinearLayout) findViewById(R.id.switch_layout);
		mSwitchLayout.setOnClickListener(this);
		mControlLayout = (LinearLayout) findViewById(R.id.control_layout);
		mControlLayout.setOnClickListener(this);
		mMoreLayout = (LinearLayout) findViewById(R.id.more_layout);
		mMoreLayout.setOnClickListener(this);
		
		mVideoImageView = (ImageView) findViewById(R.id.video_image);
		mVideoTextView = (TextView) findViewById(R.id.video_text);
		
		mHomeImageView = (ImageView) findViewById(R.id.home_image);
		mHomeTextView = (TextView) findViewById(R.id.home_text);

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		initViews();
        mFragmentManager = getFragmentManager();
		setTabSelection(0);
		
		SipLoginThread sip_login_thread = new SipLoginThread();
		sip_login_thread.start();
		
		
		/*mGridView = (GridView) findViewById(R.id.screen_home_gridview);
		mGridView.setAdapter(new ScreenHomeAdapter(this));
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final ScreenHomeItem item = (ScreenHomeItem)parent.getItemAtPosition(position);
				if (item != null) {
					if(position == ScreenHomeItem.ITEM_SIGNIN_SIGNOUT_POS){
						if(mSipService.getRegistrationState() == ConnectionState.CONNECTING || mSipService.getRegistrationState() == ConnectionState.TERMINATING){
							mSipService.stopStack();
						}
						else if(mSipService.isRegistered()){
							mSipService.unRegister();
						}
						else{
							mSipService.register(ScreenHome.this);
						}
					}
					else if (position == 3 ) //Video
					{
					    String device_sip_number = mConfigurationService.getString(NgnConfigurationEntry.Devices_SIP_NUMBER,NgnConfigurationEntry.DEFAULT_Devices_SIP_NUMBER);
					    ScreenAV.makeCall(device_sip_number,  NgnMediaType.AudioVideo);
					}
					else if (position == 4)  //Audio
					{
						String device_sip_number = mConfigurationService.getString(NgnConfigurationEntry.Devices_SIP_NUMBER,NgnConfigurationEntry.DEFAULT_Devices_SIP_NUMBER);
					    ScreenAV.makeCall(device_sip_number,  NgnMediaType.Audio);
					}
					else if(position == ScreenHomeItem.ITEM_EXIT_POS){
						final AlertDialog dialog = CustomDialog.create(
								ScreenHome.this,
								R.drawable.exit_48,
								null,
								"Are you sure you want to exit?",
								"Yes",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										((Main)(getEngine().getMainActivity())).exit();
									}
								}, "No",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								});
						dialog.show();
					}
					else{					
						mScreenService.show(item.mClass, item.mClass.getCanonicalName());
					}
				}
			}
		});*/
		
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

			break;
		case R.id.control_layout:

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
			mHomeLayout.setBackgroundResource(R.drawable.ic_home_bottom_bar_bg);
			mHomeImageView.setSelected(true);
			mHomeTextView.setTextColor(getTextColor(R.color.orange));
			if(mHomeFragment == null) {
				mHomeFragment = new HomeFragment();
				transaction.add(R.id.main_content, mHomeFragment);
			} else {
				transaction.show(mHomeFragment);
			}
			break;
		case VIDEO_INTENT_FLAG:
			clearSelection();
			mVideoLayout.setBackgroundResource(R.drawable.ic_home_bottom_bar_bg);
			mVideoImageView.setSelected(true);
			mVideoTextView.setTextColor(getTextColor(R.color.orange));
			if(mVideoFragment == null) {
				mVideoFragment = new VideoFragment();
				transaction.add(R.id.main_content, mVideoFragment);
			} else {
				transaction.show(mVideoFragment);
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
		mHomeLayout.setBackgroundDrawable(null);
		mHomeImageView.setSelected(false);
		mVideoImageView.setSelected(false);
		mHomeTextView.setTextColor(getResources().getColor(R.color.black));
		mVideoTextView.setTextColor(getResources().getColor(R.color.black));
		mVideoLayout.setBackgroundDrawable(null);
		mSwitchLayout.setBackgroundDrawable(null);
		mControlLayout.setBackgroundDrawable(null);
		mMoreLayout.setBackgroundDrawable(null);
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
		if(mTwowayVideoFragment != null) {
			transaction.hide(mTwowayVideoFragment);
		}
	}
	
	
/*	*//**
	 * ScreenHomeItem
	 *//*
	static class ScreenHomeItem {
		static final int ITEM_SIGNIN_SIGNOUT_POS = 0;
		static final int ITEM_EXIT_POS = 1;
		final int mIconResId;
		final String mText;
		final Class<? extends Activity> mClass;

		private ScreenHomeItem(int iconResId, String text, Class<? extends Activity> _class) {
			mIconResId = iconResId;
			mText = text;
			mClass = _class;
		}
	}
	
	*//**
	 * ScreenHomeAdapter
	 *//*
	static class ScreenHomeAdapter extends BaseAdapter{
		static final int ALWAYS_VISIBLE_ITEMS_COUNT = 3;
		static final ScreenHomeItem[] sItems =  new ScreenHomeItem[]{
			// always visible
    		new ScreenHomeItem(R.drawable.sign_in_48, "Sign In", null),
    		new ScreenHomeItem(R.drawable.exit_48, "Exit/Quit", null),
    		new ScreenHomeItem(R.drawable.options_48, "Options", ScreenSettings.class),
    		//new ScreenHomeItem(R.drawable.about_48, "About", ScreenAbout.class),
    		// visible only if connected
    		//new ScreenHomeItem(R.drawable.dialer_48, "Dialer", ScreenTabDialer.class),
    		//new ScreenHomeItem(R.drawable.eab2_48, "Address Book", ScreenTabContacts.class),
    		//new ScreenHomeItem(R.drawable.history_48, "History", ScreenTabHistory.class),
    		new ScreenHomeItem(R.drawable.visio_call_48, "Video", null),
    		new ScreenHomeItem(R.drawable.voice_call_48, "Audio", null),
    		//new ScreenHomeItem(R.drawable.chat_48, "Messages", ScreenTabMessages.class),
		};
		
		private final LayoutInflater mInflater;
		private final ScreenHome mBaseScreen;
		
		ScreenHomeAdapter(ScreenHome baseScreen){
			mInflater = LayoutInflater.from(baseScreen);
			mBaseScreen = baseScreen;
		}
		
		void refresh(){
			notifyDataSetChanged();
		}
		
		@Override
		public int getCount() {
			return mBaseScreen.mSipService.isRegistered() ? sItems.length : ALWAYS_VISIBLE_ITEMS_COUNT;
		}

		@Override
		public Object getItem(int position) {
			return sItems[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ScreenHomeItem item = (ScreenHomeItem)getItem(position);
			
			if(item == null){
				return null;
			}

			if (view == null) {
				view = mInflater.inflate(R.layout.screen_home_item, null);
			}
			
			if(position == ScreenHomeItem.ITEM_SIGNIN_SIGNOUT_POS){
				if(mBaseScreen.mSipService.getRegistrationState() == ConnectionState.CONNECTING || mBaseScreen.mSipService.getRegistrationState() == ConnectionState.TERMINATING){
					((TextView) view.findViewById(R.id.screen_home_item_text)).setText("Cancel");
					((ImageView) view .findViewById(R.id.screen_home_item_icon)).setImageResource(R.drawable.sign_inprogress_48);
				}
				else{
					if(mBaseScreen.mSipService.isRegistered()){
						((TextView) view.findViewById(R.id.screen_home_item_text)).setText("Sign Out");
						((ImageView) view .findViewById(R.id.screen_home_item_icon)).setImageResource(R.drawable.sign_out_48);
					}
					else{
						((TextView) view.findViewById(R.id.screen_home_item_text)).setText("Sign In");
						((ImageView) view .findViewById(R.id.screen_home_item_icon)).setImageResource(R.drawable.sign_in_48);
					}
				}
			}
			else{				
				((TextView) view.findViewById(R.id.screen_home_item_text)).setText(item.mText);
				((ImageView) view .findViewById(R.id.screen_home_item_icon)).setImageResource(item.mIconResId);
			}
			
			return view;
		}
		
	}*/
}
