package com.inhuasoft.shserver.Screens;

import com.inhuasoft.shserver.R;
import com.inhuasoft.shserver.R.layout;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.utils.NgnConfigurationEntry;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;

public class ScreenDevices extends BaseScreen {

	private final static String TAG = ScreenNatt.class.getCanonicalName();
	
	private EditText mEtDeviceSipNumber;
	
	private final INgnConfigurationService mConfigurationService;
	
	public ScreenDevices() {
		super(SCREEN_TYPE.DEVICES_T, TAG);
		mConfigurationService = getEngine().getConfigurationService();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_devices);
		
		// get controls
	    mEtDeviceSipNumber = (EditText)findViewById(R.id.screen_devices_editText_devicesipnumber);
	    
	    // load values from configuration file (do it before adding UI listeners)
	    mEtDeviceSipNumber.setText(mConfigurationService.getString(NgnConfigurationEntry.Devices_SIP_NUMBER,NgnConfigurationEntry.DEFAULT_Devices_SIP_NUMBER));
	    
	    
	    addConfigurationListener(mEtDeviceSipNumber);
	    
	    
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		
		if(super.mComputeConfiguration){ 
			mConfigurationService.putString(NgnConfigurationEntry.Devices_SIP_NUMBER, mEtDeviceSipNumber.getText().toString());
		}
		
		if(!mConfigurationService.commit()){
			Log.e(TAG, "Failed to commit() configuration");
		}
		super.mComputeConfiguration = false;
		super.onPause();
	}



}
