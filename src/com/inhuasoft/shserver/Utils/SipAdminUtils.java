package com.inhuasoft.shserver.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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

import android.R.bool;
import android.app.Activity;

import com.inhuasoft.shserver.Utils.MD5;
import com.inhuasoft.shserver.IMSDroid;
import android.os.Build;

public class SipAdminUtils {

	private Activity mActivity;

	public SipAdminUtils(Activity _mActivity) {
		mActivity = _mActivity;
	}

	public String  sip_UserName;
	public boolean sip_admin_login_flag = false;
	public boolean sip_add_user = false;

	public void Sip_Admin_Login() {
		SipAdminLoginThread thread = new SipAdminLoginThread();
		thread.start();
	}

	public boolean Sip_Add_User(String UserName) {
		sip_UserName = UserName ;
		if (!sip_admin_login_flag) {
			Sip_Admin_Login();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (sip_admin_login_flag) {
			SipAddUserThread thread = new SipAddUserThread();
			thread.start();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (sip_add_user)
			return true;
		else
			return false;

	}

	class SipAdminLoginThread extends Thread {

		public void run() {
			String login_ok = "8010210925-35110-444-10984-4122-322463124";
			String httpUrl = "http://sip.inhuasoft.cn/login.php";
			HttpPost request = new HttpPost(httpUrl);
			HttpClient httpClient = new DefaultHttpClient();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("name", "admin"));
			params.add(new BasicNameValuePair("password", "admin"));
			HttpResponse response;

			try {
				HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
				request.setEntity(entity);
				response = httpClient.execute(request);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String str = EntityUtils.toString(response.getEntity());
					String strmd5 = MD5.getMD5(str);
					System.out.println("response:" + strmd5);
					if (strmd5.equals(login_ok)) {
						System.out.println("sip admin web login success");
						sip_admin_login_flag = true;
						CookieStore cookies = ((AbstractHttpClient) httpClient)
								.getCookieStore();
						IMSDroid appCookie = ((IMSDroid) mActivity
								.getApplication());
						// ((AbstractHttpClient)
						// httpClient).setCookieStore(cookies);
						appCookie.setCookie(cookies);
						// Toast.makeText(getApplicationContext(),
						// " login success ", Toast.LENGTH_SHORT).show();
					} else {
						System.out.println("sip admin web login fail ");
						sip_admin_login_flag = false;
						// Toast.makeText(getApplicationContext(),
						// " login fail ", Toast.LENGTH_SHORT).show();
					}
				} else {
					System.out.println("sip admin web reponse fail");
					sip_admin_login_flag = false;
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class SipAddUserThread extends Thread {

		public void run() {
			String httpUrl = "http://sip.inhuasoft.cn/tools/users/user_management/user_management.php?action=add_verify&id=";
			HttpPost request = new HttpPost(httpUrl);
			HttpClient httpClient = new DefaultHttpClient();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			System.out.println("=================username:"+sip_UserName);
			params.add(new BasicNameValuePair("uname", sip_UserName));
			params.add(new BasicNameValuePair("email", sip_UserName+"@inhuasoft.cn"));
			params.add(new BasicNameValuePair("alias", sip_UserName));
			params.add(new BasicNameValuePair("domain", "115.28.9.71"));
			params.add(new BasicNameValuePair("alias_type", "dbaliases"));
			params.add(new BasicNameValuePair("passwd", sip_UserName));
			params.add(new BasicNameValuePair("confirm_passwd", sip_UserName));
			HttpResponse response;
			IMSDroid appCookie = ((IMSDroid) mActivity.getApplication());
			((AbstractHttpClient) httpClient).setCookieStore(appCookie
					.getCookie());
			try {
				HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
				request.setEntity(entity);
				response = httpClient.execute(request);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String str = EntityUtils.toString(response.getEntity());
					String strmd5 = MD5.getMD5(str);
					if (str.contains("is already a valid user")) {
						sip_add_user = true;
						System.out
								.println("=================is already a valid user");
					}
					if (str.contains("New User added!")) {
						System.out.println("New User added!");
						sip_add_user = true;
					}
					//System.out.println(" login in add user");
				} else {
					System.out.println("add user fail ");
					sip_add_user = false;
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	   public static String getDeviceNo()
	    {
	      return Build.SERIAL;
	    }

}
