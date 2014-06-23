package com.inhuasoft.shsclient.Screens;

import com.inhuasoft.shsclient.R;
import com.inhuasoft.shsclient.R.layout;
import com.inhuasoft.shsclient.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class ScreenMainAV extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_main_av);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.screen_main_av, menu);
		return true;
	}

}
