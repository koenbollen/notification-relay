package it.koen.notificationrelay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {

	private int lastMenuItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == 0) {
	        if (resultCode == RESULT_OK) {
	    		SharedPreferences settings = getSharedPreferences( NotificationAccessibility.PREF_CONTEXT, 0 );
	            String contents = intent.getStringExtra("SCAN_RESULT");
	        	Log.i("NM", " Scanned: " + contents );
	    	    switch (this.lastMenuItem) {
	    	        case R.id.menu_scan_deviceid:
	    	        	settings.edit().putString( NotificationAccessibility.PREF_DEVICEID, contents ).commit();
	    	        	Toast.makeText( this, "Device ID: " + contents, Toast.LENGTH_LONG ).show();
	    	        	break;
	    	        case R.id.menu_scan_url:
	    	        	settings.edit().putString( NotificationAccessibility.PREF_SERVER_URL, contents ).commit();
	    	        	Toast.makeText( this, "Server: " + contents, Toast.LENGTH_LONG ).show();
	    	        	break;
	    	    }
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		this.lastMenuItem = item.getItemId();
	    switch (item.getItemId()) {
	        case R.id.menu_scan_deviceid:
	        	intent = new Intent("com.google.zxing.client.android.SCAN");
	            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	            startActivityForResult(intent, 0);
	            return true;
	        case R.id.menu_scan_url:
	            intent = new Intent("com.google.zxing.client.android.SCAN");
	            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	            startActivityForResult(intent, 0);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}
