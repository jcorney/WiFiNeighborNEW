package com.boomgaarden_corney.android.wifineighbor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class WiFiNeighborMainActivity extends Activity {

	private final static String DEBUG_TAG = "DEBUG_WIFI";
	private final static String SERVER_URL = "http://54.86.68.241/wifineighbor/test.php";

	private static TextView txtResults;

	private static String errorMsg;
	private static String wifiBSSIDNeighbor;
	private static String wifiSSIDNeighbor;
	private static String wifiCapabilitiesNeighbor;

	private static int wifiRSSINeighbor;
	private static int wifiFrequencyNeighbor;
	private static int iteration = 0;

	private static int counter = 1;

	static WifiManager mWiFiManager;
	WiFiBroadcastReceiver receiverWifi;
	static List<ScanResult> wifiList;
	static StringBuilder wifiAccessPoints = new StringBuilder();

	private static List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsWiFi = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wi_fi_neighbor_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);
		mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		receiverWifi = new WiFiBroadcastReceiver();
		registerReceiver(receiverWifi, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mWiFiManager.startScan();

		setDeviceData();
		showDeviceData();
		// sendDeviceData();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wi_fi_neighbor_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("WIFI")) {
			writer.write(buildPostRequest(paramsWiFi));
			paramsWiFi = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Telephony
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {

		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));

	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");
	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	public static class WiFiBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (counter <= 10) {
				wifiList = mWiFiManager.getScanResults();
				for (ScanResult result : wifiList) {
					wifiBSSIDNeighbor = result.BSSID;
					wifiSSIDNeighbor = result.SSID;
					wifiCapabilitiesNeighbor = result.capabilities;
					wifiRSSINeighbor = result.level;
					wifiFrequencyNeighbor = result.frequency;

					paramsWiFi.add(new BasicNameValuePair(
							"-----NEIGHBOR NETWORK INFORMATION-----" + iteration, " "));
					paramsWiFi.add(new BasicNameValuePair("Count: " , String
							.valueOf(counter)));
					paramsWiFi.add(new BasicNameValuePair("SSID: " + iteration,
							wifiSSIDNeighbor));
					paramsWiFi.add(new BasicNameValuePair("BSSID: " + iteration,
							wifiBSSIDNeighbor));
					paramsWiFi.add(new BasicNameValuePair("Capabilities: " + iteration,
							wifiCapabilitiesNeighbor));
					paramsWiFi.add(new BasicNameValuePair(
							"Received Signal Strength Indicator (RSSI): " + iteration,
							String.valueOf(wifiRSSINeighbor)));
					paramsWiFi.add(new BasicNameValuePair("Frequency: " + iteration, String
							.valueOf(wifiFrequencyNeighbor)));
					iteration++;
					showWiFiData();
				}
				counter++;				
				sendWiFiData(context);
			} 


		}

		private void sendWiFiData(Context context) {
			ConnectivityManager connectMgr = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

			// Verify network connectivity is working; if not add note to
			// TextView
			// and Logcat file
			if (networkInfo != null && networkInfo.isConnected()) {
				// Send HTTP POST request to server which will include POST
				// parameters with Telephony info
				new SendHttpRequestTask().execute(SERVER_URL, "WIFI");
			} else {
				setErrorMsg("No Network Connectivity");
				showErrorMsg();
			}
		}

		private void setErrorMsg(String error) {
			errorMsg = error;
			paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
		}

		private void showErrorMsg() {
			Log.d(DEBUG_TAG, errorMsg);
			txtResults.append(errorMsg + "\n");
		}

		private void showWiFiData() {
			StringBuilder results = new StringBuilder();

			results.append("-----NEIGHBOR NETWORK INFORMATION-----\n");
			results.append("Count: " + counter + "\n");
			results.append("SSID: " + wifiSSIDNeighbor + "\n");
			results.append("BSSID: " + wifiBSSIDNeighbor + "\n");
			results.append("Capabilities: " + wifiCapabilitiesNeighbor + "\n");
			results.append("Received Signal Strength Indicator (RSSI): "
					+ wifiRSSINeighbor + "\n");
			results.append("Frequency: " + wifiFrequencyNeighbor + "MHz\n");

			txtResults.append(new String(results));
			txtResults.append("\n");
		}

		private String sendHttpRequest(String myURL, String postParameters)
				throws IOException {

			URL url = new URL(myURL);

			// Setup Connection
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000); /* in milliseconds */
			conn.setConnectTimeout(15000); /* in milliseconds */
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			// Setup POST query params and write to stream
			OutputStream ostream = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					ostream, "UTF-8"));

			if (postParameters.equals("DEVICE")) {
				writer.write(buildPostRequest(paramsDevice));
			} else if (postParameters.equals("WIFI")) {
				writer.write(buildPostRequest(paramsWiFi));
				paramsWiFi = new ArrayList<NameValuePair>();
			} else if (postParameters.equals("ERROR_MSG")) {
				writer.write(buildPostRequest(paramsErrorMsg));
				paramsErrorMsg = new ArrayList<NameValuePair>();
			}

			writer.flush();
			writer.close();
			ostream.close();

			// Connect and Log response
			conn.connect();
			int response = conn.getResponseCode();
			Log.d(DEBUG_TAG, "The response is: " + response);

			conn.disconnect();

			return String.valueOf(response);

		}

		private class SendHttpRequestTask extends
				AsyncTask<String, Void, String> {

			// @params come from SendHttpRequestTask.execute() call
			@Override
			protected String doInBackground(String... params) {
				// params comes from the execute() call: params[0] is the url,
				// params[1] is type POST
				// request to send - i.e. whether to send Device or
				// Telephony
				// parameters.
				try {
					return sendHttpRequest(params[0], params[1]);
				} catch (IOException e) {
					setErrorMsg("Unable to retrieve web page. URL may be invalid.");
					showErrorMsg();
					return errorMsg;
				}
			}
		}
	}

	protected void onPause() {
		unregisterReceiver(receiverWifi);
		super.onPause();
	}

	protected void onResume() {
		registerReceiver(receiverWifi, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		super.onResume();
	}

}
