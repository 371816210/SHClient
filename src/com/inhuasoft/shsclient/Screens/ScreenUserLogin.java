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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.inhuasoft.shsclient.CustomDialog;
import com.inhuasoft.shsclient.Engine;
import com.inhuasoft.shsclient.IMSDroid;
import com.inhuasoft.shsclient.Main;
import com.inhuasoft.shsclient.Services.IScreenService;
import com.inhuasoft.shsclient.Utils.MD5;
import com.inhuasoft.shsclient.Utils.RegexUtils;
import com.inhuasoft.shsclient.Utils.SipAdminUtils;
import com.inhuasoft.shsclient.R;
import com.inhuasoft.shsclient.R.drawable;
import com.inhuasoft.shsclient.R.id;
import com.inhuasoft.shsclient.R.layout;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnSipSession.ConnectionState;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnPredicate;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.tinyWRAP.MsrpCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.R.integer;
import android.app.Activity;
import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ScreenUserLogin extends BaseScreen implements OnClickListener {
	private static String TAG = ScreenLogin.class.getCanonicalName();

	private final INgnConfigurationService mConfigurationService;
	private final INgnSipService mSipService;
	
	Button btnSubmit;
	EditText editUserName, editPassword;
	TextView txtMsginfo;
	CheckBox chkMsginfo;

    private String  DeviceNo ;
	private static final int StartMain = 0x29;
	private static final int User_Login_Action = 0x30;
	private static final int User_Login_Success = 0x31;
	private static final int User_Login_Fail = 0x32;

	public ScreenUserLogin() {
		super(SCREEN_TYPE.SCREENUSERLOGIN, TAG);
		// Sets main activity (should be done before starting services)

		mConfigurationService = ((Engine) Engine.getInstance())
				.getConfigurationService();
		mSipService = getEngine().getSipService();
	}

	 private void  SetSystemInfo() {
		 mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU,"sip:"+editUserName.getText().toString()+"@115.28.9.71");
		 mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, editUserName.getText().toString());
		 mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_DISPLAY_NAME, editUserName.getText().toString());
		 mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, editPassword.getText().toString());
		 mConfigurationService.putBoolean(NgnConfigurationEntry.USER_LOGIN, true);
		 
		 mConfigurationService.putString(NgnConfigurationEntry.USERNAME,editUserName.getText().toString());
		 mConfigurationService.putString(NgnConfigurationEntry.USER_PASSWORD,editPassword.getText().toString());
		 mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_DISCOVERY,NgnConfigurationEntry.DEFAULT_NETWORK_PCSCF_DISCOVERY);
		 mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST,NgnConfigurationEntry.DEFAULT_NETWORK_PCSCF_HOST);
		 mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT,NgnConfigurationEntry.DEFAULT_NETWORK_PCSCF_PORT);
		 mConfigurationService.putString(NgnConfigurationEntry.NETWORK_TRANSPORT,NgnConfigurationEntry.DEFAULT_NETWORK_TRANSPORT);
		 mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_WIFI,true);
		 mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G,true);
		 mConfigurationService.putString(NgnConfigurationEntry.NETWORK_IP_VERSION,"ipv4");
		 mConfigurationService.putString(NgnConfigurationEntry.Devices_SIP_NUMBER, "sip:"+DeviceNo+"@115.28.9.71");
		 if(!mConfigurationService.commit()){
				Log.e(TAG, "Failed to Commit() configuration");
			}
		 
	}
	 
	 private void  StartMain() {
		 if(mScreenService != null )
		 {
			 try {
				    SetSystemInfo();
				    mScreenService.show(ScreenHome.class);
				    finish();
			} catch (Exception e) {
				// TODO: handle exception
			}
			
		 }
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case StartMain:
				StartMain();
				break;
			case User_Login_Action:
				UserLoginThread thread_user_login = new UserLoginThread();
				thread_user_login.start();
				break;
			case User_Login_Success:
				StartMain();
				break;
			case User_Login_Fail:
				int user_login_fail_errorcode = msg.arg1;
				if(user_login_fail_errorcode == 802)
				{
				final AlertDialog user_login_fail_dialog = CustomDialog
						.create(ScreenUserLogin.this, R.drawable.exit_48, null,
								" The user name or password is not correct. ", "exit",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										//ScreenLogin.this.finish();
										ResetInput();

									}
								}, null, null);
				user_login_fail_dialog.show(); 
				}
				else 
				{
					final AlertDialog user_login_fail_dialog = CustomDialog
							.create(ScreenUserLogin.this, R.drawable.exit_48, null,
									" A error has occurred,the error code is  "
											+ user_login_fail_errorcode, "exit",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog,
												int which) {
											//ScreenLogin.this.finish();
											ResetInput();

										}
									}, null, null);
					user_login_fail_dialog.show(); 
				}
				break;
			default:
				super.handleMessage(msg);
				break;

			}
		}
	};

	public static String getDeviceNo() {
		return Build.SERIAL;
	}

	public static String getValByTagName(Document doc, String tagName) {
		NodeList list = doc.getElementsByTagName(tagName);
		if (list.getLength() > 0) {
			Node node = list.item(0);
			Node valNode = node.getFirstChild();
			if (valNode != null) {
				String val = valNode.getNodeValue();
				return val;
			}
		}
		return null;
	}

	public static String ParserXml(String xml, String nodeName) {
		String returnStr = null;
		ByteArrayInputStream tInputStringStream = null;
		try {
			if (xml != null && !xml.trim().equals("")) {
				tInputStringStream = new ByteArrayInputStream(xml.getBytes());
			}
		} catch (Exception e) {
			return null;
		}
		XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setInput(tInputStringStream, "UTF-8");
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:// ��ʼԪ���¼�
					String name = parser.getName();
					if (name.equalsIgnoreCase(nodeName)) {
						returnStr = parser.nextText();
					}
					break;
				}
				eventType = parser.next();
			}
			tInputStringStream.close();
			// return persons;
		} catch (XmlPullParserException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return returnStr;
	}
	
	class UserLoginThread extends Thread {

		public void run() {
			String RequestUrl = "http://ota.inhuasoft.cn/SHS_WS/ShsService.asmx?op=GetUserByDevice";
			URL url = null;
			try {
				url = new URL(RequestUrl);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				Message message = mHandler.obtainMessage(User_Login_Fail);
				message.arg1 = 701;
				message.sendToTarget();
				e1.printStackTrace();
			}
			String envelope = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+ "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
					+ "<soap12:Header>"
					+ "<MySoapHeader xmlns=\"http://tempuri.org/\">"
					+ "<UserName>"+editUserName.getText().toString()+"</UserName>"
					+ "<PassWord>"+editPassword.getText().toString()+"</PassWord>"
					+ "</MySoapHeader>" 
					+ "</soap12:Header>" 
					+ "<soap12:Body>"
					+ "<GetDevicesByUser xmlns=\"http://tempuri.org/\">"
				    + "<UserName>"+editUserName.getText().toString()+"</UserName>"
				    + "</GetDevicesByUser>" 
					+ "</soap12:Body>"
					+ "</soap12:Envelope>";
			HttpURLConnection httpConnection = null;
			OutputStream output = null;
			InputStream input = null;
			try {
				httpConnection = (HttpURLConnection) url.openConnection();
				httpConnection.setRequestMethod("POST");
				httpConnection.setRequestProperty("Content-Length",
						String.valueOf(envelope.length()));
				httpConnection.setRequestProperty("Content-Type",
						"text/xml; charset=utf-8");
				httpConnection.setDoOutput(true);
				httpConnection.setDoInput(true);
				output = httpConnection.getOutputStream();
				output.write(envelope.getBytes());
				output.flush();
				input = httpConnection.getInputStream();
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document dom = builder.parse(input);
				String returncode = getValByTagName(dom,
						"GetDevicesByUserResult");// ������
				System.out
						.println("======GetDevicesByUserResult======== return code is  "
								+ returncode);
				if ("-2".equals(returncode)) {
					Message message = mHandler.obtainMessage(User_Login_Fail);
					message.arg1 = 802;
					message.sendToTarget();
				} 
				else 
				{
				     DeviceNo = ParserXml(returncode, "DeviceNo");
					if (DeviceNo != null  && !"".equals(DeviceNo)) {
						Message message = mHandler.obtainMessage(User_Login_Success);
						message.sendToTarget();
					}
					else {
						Message message = mHandler.obtainMessage(User_Login_Fail);
						message.arg1 = 808;
						message.sendToTarget();
					}
					
				}

			} catch (Exception ex) {
				Log.d(TAG, "-->getResponseString:catch" + ex.getMessage());
				Message message = mHandler.obtainMessage(User_Login_Fail);
				message.arg1 = 806;
				message.sendToTarget();
			} finally {
				try {
					output.close();
					input.close();
					httpConnection.disconnect();
				} catch (Exception e) {
					Log.d(TAG, "-->getResponseString:finally" + e.getMessage());
					Message message = mHandler
							.obtainMessage(User_Login_Fail);
					message.arg1 = 807;
					message.sendToTarget();
				}
			}

		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_user_login);

		btnSubmit = (Button) findViewById(R.id.btnsubmit);
		btnSubmit.setOnClickListener(this);
		editUserName = (EditText) findViewById(R.id.edit_username);
		editPassword = (EditText) findViewById(R.id.edit_password);
	
 
    	

	}

	@Override
	protected void onDestroy() {
		/*
		 * if(mSipBroadCastRecv != null){ unregisterReceiver(mSipBroadCastRecv);
		 * mSipBroadCastRecv = null; }
		 */
		super.onDestroy();
	}

	private void ResetInput() {
		editUserName.setText("");
		editPassword.setText("");
	}
	

	public void SetLoginUI()
	{
		txtMsginfo.setVisibility(View.GONE);
		chkMsginfo.setVisibility(View.GONE);
		btnSubmit.setText("Sign In");
	}
	
	
	
	private boolean ValidateLoginInput() {
		if (!RegexUtils.checkUserName(editUserName.getText().toString())) {
			final AlertDialog username_error_dialog = CustomDialog.create(
					ScreenUserLogin.this, R.drawable.exit_48, null,
					" The username is 4-16 any combination of characters ",
					"OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}, null, null);
			username_error_dialog.show();
			ResetInput();
			return false;
		}
		if (!RegexUtils.checkPassWord(editPassword.getText().toString())) {
			final AlertDialog password_error_dialog = CustomDialog.create(
					ScreenUserLogin.this, R.drawable.exit_48, null,
					" The password is 6-16 any combination of characters ",
					"OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}, null, null);
			password_error_dialog.show();
			editPassword.setText("");
			return false;
		}

		return true;
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if (arg0.getId() == R.id.btnsubmit) {
			if(ValidateLoginInput())
			{
			  Message message = mHandler.obtainMessage(User_Login_Action);
			  message.sendToTarget();	
			}
			
		}
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		final Engine engine = getEngine();
			
		final Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				if(!engine.isStarted()){
					Log.d(TAG, "Starts the engine from the splash screen");
					engine.start();
				}
			}
		});
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

}
