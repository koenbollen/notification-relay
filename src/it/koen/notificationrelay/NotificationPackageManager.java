package it.koen.notificationrelay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class NotificationPackageManager
{
	private SharedPreferences settings;
	private Set<String> packages;
	private Map<String, String> names;
	private Map<String, Boolean> ignored;

	public NotificationPackageManager( Context context )
	{
		this.settings = context.getSharedPreferences( NotificationAccessibility.PREF_CONTEXT, 0 );
		this.packages = new HashSet<String>();
		this.names = new HashMap<String, String>();
		this.ignored = new HashMap<String, Boolean>();
		this.load();
	}
	
	private void load()
	{
		int size = this.settings.getInt( "size", 0 );
		for( int i = 0; i < size; i++ ) {
			String p = this.settings.getString( "package"+i, null );
			String a = this.settings.getString( "application"+i, null );
			boolean ig = this.settings.getBoolean( "ignored"+i, false );
			if( p != null && a != null) {
				this.packages.add( p );
				this.names.put( p, a );
				this.ignored.put( p, ig );
			}
		}
	}

	public List<String> list()
	{
		return asSortedList( this.packages );
	}
	
	public String applicationName( String packagename ) {
		return this.names.get(packagename);
	}

	public void ignore( String packagename ) {
		this.load();
		this.ignored.put( packagename, true );
		this.save();
	}
	public void unignore( String packagename ) {
		this.load();
		this.ignored.put( packagename, false );
		this.save();
	}
	public boolean isIgnored( String packagename ) {
		this.load();
		if( !this.ignored.containsKey( packagename ) )
			return false;
		return this.ignored.get( packagename );
	}
	
	private void save()
	{
		Editor e = this.settings.edit();
		e.putInt( "size", this.packages.size() );
		int i = 0;
		for( String s : this.packages ) {
			e.putString( "package"+i, s );
			e.putString( "application"+i, this.names.get(s) );
			e.putBoolean( "ignored"+i, this.ignored.get(s) );
			i++;
		}
		e.commit();
	}
	
	public void touch( String packagename, String applicationName )
	{
		this.load();
		packagename = packagename.toLowerCase().trim();
		this.packages.add( packagename );
		this.names.put( packagename, applicationName );
		this.save();
	}
	
	public static
	<T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
	  List<T> list = new ArrayList<T>(c);
	  java.util.Collections.sort(list);
	  return list;
	}
}
