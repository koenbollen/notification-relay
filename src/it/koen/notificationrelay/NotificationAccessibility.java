package it.koen.notificationrelay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

public class NotificationAccessibility extends AccessibilityService
{
	public static final String PREF_CONTEXT = "NotificationMasterPreferences";
	public static final String PREF_SERVER_URL = "NotificationServerUrl";
	public static final String PREF_DEVICEID = "NotificationDeviceID";

	private boolean initialized;
	private SharedPreferences settings;
	private DefaultHttpClient httpclient;
	private NotificationPackageManager nmp;

	@Override
	public void onAccessibilityEvent( AccessibilityEvent event )
	{
		if( event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED )
		{
			final String url = settings.getString( PREF_SERVER_URL, null );
			final String deviceid = settings.getString( PREF_DEVICEID, null );
			if( url == null )
			{
				Log.w( "NM", "Missing server url, not sending." );
				return;
			}
			if( deviceid == null )
			{
				Log.w( "NM", "Missing device ID, not sending." );
				return;
			}
			final String packagename = String.valueOf( event.getPackageName() );
			final String text = getEventText( event );
			final Notification n = (Notification) event.getParcelableData();
			this.nmp.touch( packagename, getApplicationName( packagename ) );
			if(this.nmp.isIgnored( packagename ))
				return;

			JSONObject json = buildJSON( packagename, text, n );
			if( json == null )
				return;
			Log.d( "NM", json.toString() );
			sendNotification( json, url, deviceid );
		}
	}

	private void sendNotification( JSONObject json, String url, String deviceid )
	{
		// TODO: Make this method async.
		try
		{
			json.put( "deviceid", deviceid );
		} catch( JSONException e )
		{
			return;
		}
		HttpPost httppost = new HttpPost( url );

		try
		{
			httppost.setEntity( new StringEntity( json.toString(), "UTF8" ) );
			httppost.setHeader( "Content-Type", "application/json" );

			httpclient.execute( httppost );

		} catch( ClientProtocolException e )
		{
			Log.e( "NM", "Failed to post notification JSON data", e );
		} catch( IOException e )
		{
			Log.e( "NM", "Failed to post notification JSON data", e );
		}

		Log.d( "NM", "Sent notification." );
	}

	private JSONObject buildJSON( String packagename, String eventtext, Notification notification )
	{
		if( packagename == null || notification == null )
			return null;
		JSONObject json = null;
		try
		{
			List<String> real = getRealText( notification );
			json = new JSONObject();
			json.put( "package", packagename );
			json.put( "application", getApplicationName( packagename ) );
			if( real == null )
			{
				json.put( "title", getApplicationName( packagename ) );
				json.put( "text", eventtext );
			} else
			{
				json.put( "title", real.get( 0 ) );
				json.put( "text", real.get( 1 ) );
			}
			json.put( "eventtext", eventtext );
			if( notification.vibrate != null )
			{
				json.put( "vibrate", new JSONArray() );
				for( long l : notification.vibrate )
					json.getJSONArray( "vibrate" ).put( l );
			}
			json.put( "sound", notification.sound );
			json.put( "defaults", notification.defaults );
			json.put( "flags", notification.flags );
			json.put( "time", (int) (System.currentTimeMillis() / 1000) );
			if( notification.ledOnMS != 0 || notification.ledOffMS != 0 || notification.ledARGB != 0 )
			{
				json.put( "led", new JSONObject() );
				json.getJSONObject( "led" ).put( "argb", notification.ledARGB );
				json.getJSONObject( "led" ).put( "offms", notification.ledOffMS );
				json.getJSONObject( "led" ).put( "onms", notification.ledOnMS );
			}
			addIcon( json, notification );
		} catch( JSONException e )
		{
			Log.e( "NM", "Failed to create JSON object.", e );
			return null;
		}
		return json;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void addIcon( JSONObject json, Notification n ) throws JSONException
	{
		if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			n.largeIcon.compress( Bitmap.CompressFormat.PNG, 100, baos );
			String data = Base64.encodeToString( baos.toByteArray(), Base64.DEFAULT );
			json.put( "icon", data );
		}
	}

	private List<String> getRealText( Notification notification )
	{
		RemoteViews views = notification.contentView;
		Class<?> secretClass = views.getClass();

		try
		{
			SparseArray<String> textmap = new SparseArray<String>();

			Field outerFields[] = secretClass.getDeclaredFields();
			for( int i = 0; i < outerFields.length; i++ )
			{
				if( !outerFields[i].getName().equals( "mActions" ) )
					continue;

				outerFields[i].setAccessible( true );

				ArrayList<?> actions = (ArrayList<?>) outerFields[i].get( views );
				for( Object action : actions )
				{
					Field innerFields[] = action.getClass().getDeclaredFields();

					Object value = null;
					Integer type = null;
					Integer viewId = null;
					for( Field field : innerFields )
					{
						field.setAccessible( true );
						if( field.getName().equals( "value" ) )
						{
							value = field.get( action );
						} else if( field.getName().equals( "type" ) )
						{
							type = field.getInt( action );
						} else if( field.getName().equals( "viewId" ) )
						{
							viewId = field.getInt( action );
						}
					}

					if( type == 9 || type == 10 )
					{
						Log.d( "NM", viewId + " OMG: " + value.toString() );
						textmap.put( viewId, value.toString() );
					}
				}

				String title = textmap.get( 16908310 );
				String text = textmap.get( 16908354 );
				return Arrays.asList( title, text );
			}
			return null;
		} catch( Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void onServiceConnected()
	{
		if( this.initialized )
		{
			return;
		}
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.flags = AccessibilityServiceInfo.DEFAULT;
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
		setServiceInfo( info );

		this.settings = getSharedPreferences( PREF_CONTEXT, 0 );
		final String url = settings.getString( PREF_SERVER_URL, null );
		final String deviceid = settings.getString( PREF_DEVICEID, null );
		Log.i( "NM", "Settings: " + url + " " + deviceid );

		this.httpclient = new DefaultHttpClient();
		this.httpclient.getParams().setParameter( CoreProtocolPNames.USER_AGENT, "NotiticationMaster/0.1 (Koen Bollen)" );

		this.nmp = new NotificationPackageManager( this );
		
		this.initialized = true;
		Log.i( "NM", "Notification Service Connected" );
	}

	@Override
	public void onInterrupt()
	{
		this.initialized = false;
	}

	private String getApplicationName( String packagename )
	{
		final PackageManager pm = getApplicationContext().getPackageManager();
		ApplicationInfo ai;
		try
		{
			ai = pm.getApplicationInfo( packagename, 0 );
		} catch( final NameNotFoundException e )
		{
			ai = null;
		}
		return (String) (ai != null ? pm.getApplicationLabel( ai ) : "(unknown)");
	}

	private String getEventText( AccessibilityEvent event )
	{
		StringBuilder sb = new StringBuilder();
		for( CharSequence s : event.getText() )
		{
			sb.append( s );
		}
		return sb.toString();
	}
}