package it.koen.notificationrelay;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private int lastMenuItem;

	private NotificationPackageManager nmp;
	private List<String> currentPackagesList;
	
	private OnItemClickListener mMessageClickedHandler = new OnItemClickListener() {
	    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	    	ListView listView = (ListView) findViewById(R.id.packages);
	    	boolean checked = listView.isItemChecked( position );
	    	Log.d( "NM", "YEA! CLICK " + position + " " + checked );
	    	String ms;
	    	String p = MainActivity.this.currentPackagesList.get(position);
	    	if(checked)
	    	{
	    		MainActivity.this.nmp.ignore(MainActivity.this.currentPackagesList.get(position));
	    		ms = MainActivity.this.nmp.applicationName( p ) + " ignored";
	    	}
	    	else
	    	{
	    		MainActivity.this.nmp.unignore(MainActivity.this.currentPackagesList.get(position));
	    		ms = MainActivity.this.nmp.applicationName( p ) + " unignored";
	    	}
	    	Toast.makeText( MainActivity.this, ms, Toast.LENGTH_SHORT ).show();
	    }
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.nmp = new NotificationPackageManager(this);

		this.currentPackagesList = this.nmp.list();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, currentPackagesList );
		ListView listView = (ListView) findViewById(R.id.packages);
		listView.setAdapter(adapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(mMessageClickedHandler); 
	}
	
	@Override
	protected void onResume()
	{
		ListView listView = (ListView) findViewById(R.id.packages);
		this.currentPackagesList = this.nmp.list();
		for( int i = 0; i <this.currentPackagesList.size(); i++ ) {
			Log.d("NM", "init: " + i + " " + this.nmp.isIgnored( this.currentPackagesList.get(i) ));
			listView.setItemChecked( i, this.nmp.isIgnored( this.currentPackagesList.get(i) ) );
		}
		super.onResume();
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
	        	Log.i("NM", "Scanned: " + contents );
	    	    switch (this.lastMenuItem) {
	    	        case R.id.menu_scan_deviceid:
	    	        	Log.d("NM", "Setting device ID: " + contents);
	    	        	settings.edit().putString( NotificationAccessibility.PREF_DEVICEID, contents ).commit();
	    	        	Toast.makeText( this, "Device ID: " + contents, Toast.LENGTH_LONG ).show();
	    	        	break;
	    	        case R.id.menu_scan_url:
	    	        	Log.d("NM", "Setting server url: " + contents);
	    	        	settings.edit().putString( NotificationAccessibility.PREF_SERVER_URL, contents ).commit();
	    	        	Toast.makeText( this, "Server: " + contents, Toast.LENGTH_LONG ).show();
	    	        	break;
	    	    }
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
