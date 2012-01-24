/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import java.awt.AWTEvent;
import java.io.File;

/**
 * The event that passes a message and done flag to SenderListeners.
 */
public class SenderEvent extends AWTEvent {

	public static final int SENDER_EVENT = AWTEvent.RESERVED_ID_MAX + 4269;


	/** The message that has been selected. */
	public String message;
	/** The flag indicating whether all files have been sent. */
	public boolean done;

	/**
	 * Class constructor capturing the message to be sent
	 * and setting the done flag to false.
	 * @param object the source of the event.
	 * @param message the message to be sent.
	 */
	public SenderEvent(Object object, String message) {
		super(object, SENDER_EVENT);
		this.message = message;
		this.done = false;
	}

	/**
	 * Class constructor sending an empty message
	 * and setting the done flag to true.
	 * @param object the source of the event.
	 */
	public SenderEvent(Object object) {
		super(object, SENDER_EVENT);
		this.message = "";
		this.done = true;
	}

	/**
	 * Class constructor capturing the message
	 * and setting the done flag.
	 * @param object the source of the event.
	 * @param message the message to be sent.
	 * @param done true if all files have been sent; false otherwise.
	 */
	public SenderEvent(Object object, String message, boolean done) {
		super(object, SENDER_EVENT);
		this.message = message;
		this.done = done;
	}

}
