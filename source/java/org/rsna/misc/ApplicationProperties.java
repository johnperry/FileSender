/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.misc;

import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;

/**
 * An extension of java.util.Properties that provides PropertyEvents
 * to listeners.
 */
public class ApplicationProperties extends Properties {

	String filename = "application.properties";
	EventListenerList listenerList;
	boolean notifyOnChange = false;

	/**
	 * Class constructor; creates a new Properties object with
	 * automatic notification on changes turned off and
	 * loads the properties file, ignoring exceptions.
	 *
	 * @param filename the path to the properties file.
	 */
	public ApplicationProperties(String filename) {
		this(filename,false);
	}

	/**
	 * Class constructor; creates a new Properties object with
	 * the specified automatic notification on changes and
	 * loads the properties file, ignoring exceptions.
	 *
	 * @param filename the path to the properties file.
	 * @param notifyOnChange true if every property change is
	 * to generate PropertyEvents; false otherwise.
	 */
	public ApplicationProperties(String filename, boolean notifyOnChange) {
		super();
		this.filename = filename;
		this.notifyOnChange = notifyOnChange;
		listenerList = new EventListenerList();
		load();
	}

	/**
	 * Get the filename.
	 * @return the filename.
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Load the properties file, and if notifyOnChange is set,
	 * notify the listeners.
	 * @return true if the load was successful; false otherwise.
	 */
	public boolean load() {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(filename);
			super.load(stream);
			stream.close();
			if (notifyOnChange) notifyListeners();
			return true;
		}
		catch (Exception e) {
			if (stream != null) {
				try { stream.close(); }
				catch (Exception ignore) { }
			}
			return false;
		}
	}

	/**
	 * Save the properties file.
	 * @return true if the save was successful; false otherwise.
	 */
	public boolean store() {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(filename);
			super.store(stream,filename);
			stream.flush();
			stream.close();
			return true;
		}
		catch (Exception e) {
			if (stream != null) {
				try { stream.close(); }
				catch (Exception ignore) { }
			}
			return false;
		}
	}

	/**
	 * Set the notifyOnChange flag.
	 * @param notifyOnChange true if every property change is
	 * to generate PropertyEvents; false otherwise.
	 */
	public void setNotifyOnChange(boolean notifyOnChange) {
		this.notifyOnChange = notifyOnChange;
	}

	/**
	 * Set a property, and if notifyOnChange is set, send
	 * PropertyEvents to the listeners.
	 * @return the previous value of the property, or null if the
	 * property had no previous value.
	 */
	public Object setProperty(String key, String value) {
		Object object = super.setProperty(key,value);
		if (notifyOnChange) sendPropertyEvent(this,key,value,(String)object);
		return object;
	}

	/**
	 * Add a PropertyListener to the listener list.
	 * @param listener the PropertyListener.
	 */
	public void addPropertyListener(PropertyListener listener) {
		listenerList.add(PropertyListener.class, listener);
	}

	/**
	 * Send a PropertyEvent to the PropertyListeners, encouraging
	 * them to check the properties object.
	 */
	public void notifyListeners() {
		sendPropertyEvent(this, null, null, null);
	}

	/**
	 * Remove a PropertyListener from the listener list.
	 * @param listener the PropertyListener.
	 */
	public void removePropertyListener(PropertyListener listener) {
		listenerList.remove(PropertyListener.class, listener);
	}

	/**
	 * Send a PropertyEvent to all PropertyListeners. This method sends the PropertyEvent
	 * in the Event thread to make it thread-safe for GUI components.
	 * @param object the Object sending the PropertyEvent.
	 * @param key the property that changed.
	 * @param newValue the new value of the property.
	 * @param oldValue the old value of the property.
	 */
	public void sendPropertyEvent(Object object, String key, String newValue, String oldValue) {
		final PropertyEvent event = new PropertyEvent(object,key,newValue,oldValue);
		final EventListener[] listeners = listenerList.getListeners(PropertyListener.class);
		Runnable fireEvents = new Runnable() {
			public void run() {
				for (int i=0; i<listeners.length; i++) {
					((PropertyListener)listeners[i]).propertyChanged(event);
				}
			}
		};
		SwingUtilities.invokeLater(fireEvents);
	}

}
