/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.*;
import javax.swing.*;
import org.apache.log4j.*;
import org.rsna.ui.ApplicationProperties;
import org.rsna.ui.GeneralAuthenticator;
import org.rsna.ui.SourcePanel;

/**
 * The FileSender program sends files using the HTTP, HTTPS,
 * or DICOM protocols. It can send individual files, all the
 * files in a directory, or all the files in a directory and
 * subdirectories, with file filter matching.
 */
public class FileSender extends JFrame {

	String windowTitle = "MIRC FileSender - version 25";
	ApplicationProperties properties;
	GeneralAuthenticator authenticator;
	RightPanel rightPanel;
	static Logger logger = null;

	public static void main(String args[]) {
		File logProps = new File("log4j.properties");
		String propsPath = logProps.getAbsolutePath();
		if (!logProps.exists()) {
			System.out.println("Logger configuration file: "+propsPath);
			System.out.println("Logger configuration file not found.");
		}
		PropertyConfigurator.configure(propsPath);
		logger = Logger.getLogger(FileSender.class);
		
		new FileSender();
	}

	/**
	 * Class constructor; creates a new instance of the FileSender class.
	 */
	public FileSender() {
		properties = new ApplicationProperties(new File("FileSender.properties"));
		authenticator = new GeneralAuthenticator(this);
		Authenticator.setDefault(authenticator);

		setTitle(windowTitle);
		addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent evt) { exitForm(evt); } });

		SourcePanel sourcePanel = new SourcePanel(properties);
		rightPanel = new RightPanel(properties,sourcePanel);
		JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,sourcePanel,rightPanel);
		jSplitPane.setResizeWeight(0.5D);
		jSplitPane.setContinuousLayout(true);
		getContentPane().add(jSplitPane, BorderLayout.CENTER);

		pack();

		Toolkit t = getToolkit();
		Dimension scr = t.getScreenSize ();
		setSize(scr.width*3/4, scr.height/2);
		setLocation (new Point ((scr.width-getSize().width)/2,(scr.height-getSize().height)/2));
		setVisible(true);
	}

	//Store the properties object before exiting.
	private void exitForm(java.awt.event.WindowEvent evt) {
		rightPanel.save();
		properties.store();
		System.exit(0);
	}
}
