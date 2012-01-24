/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * A JScrollPane containing a JEditorPane set to display text/html
 * and not to be editable. The editor starts containing no text,
 * and provides an append method to add text. No capability is
 * provided to replace the text.
 */
public class SenderScrollPane extends JScrollPane {

	JEditorPane text;
	StringBuffer sb;

	/**
	 * Class constructor; creating an instance of the SenderScrollPane.
	 */
	public SenderScrollPane() {
		super();
		this.getVerticalScrollBar().setUnitIncrement(25);
		sb = new StringBuffer();
		text = new JEditorPane();
		text.setContentType("text/html");
		text.setEditable(false);
		setViewportView(text);
	}

	/**
	 * Append a String to the text currently in the text buffer
	 * and display the entire buffer.
	 * @param string the string to be appended.
	 */
	public void append(String string) {
		sb.append(string);
		text.setText(sb.toString());
	}

	/**
	 * Append a String to the text currently in the text buffer
	 * and display just the String (not the entire buffer).
	 * @param string the string to be appended.
	 */
	public void appendString(String string) {
		sb.append(string);
		text.setText(string);
	}

	/**
	 * Append a String to the text currently in the text buffer
	 * only if the string does not contain "OK". Then
	 * display just the String (not the entire buffer).
	 * @param string the string to be appended.
	 */
	public void appendErrorString(String string) {
		if (string.indexOf("OK") == -1) sb.append(string);
		text.setText(string);
	}

	/**
	 * Display all the text in the buffer.
	 */
	public void displayAll() {
		if (sb.length() == 0) sb.append("No errors.");
		else text.setText(sb.toString());
	}

}
