/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import org.rsna.installer.SimpleInstaller;

/**
 * The FileSender program installer, consisting of just a
 * main method that instantiates a SimpleInstaller.
 */
public class Installer {

	static String windowTitle = "FileSender Installer";
	static String programName = "FileSender";
	static String introString = "<p><b>FileSender</b> is a test tool for transmitting files "
								+ "via the HTTP, HTTPS, and DICOM protocols.</p>"
								+ "<p>This program installs and configures the software components "
								+ "required to transmit images and other files for testing a MIRC site "
								+ "or clinical trial field center application.</p>";

	public static void main(String args[]) {
		new SimpleInstaller(windowTitle,programName,introString);
	}
}