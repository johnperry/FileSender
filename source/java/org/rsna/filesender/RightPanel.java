/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.rsna.misc.ApplicationProperties;
import org.rsna.misc.FileEvent;
import org.rsna.misc.FileListener;
import org.rsna.misc.SourcePanel;

/**
 * A JPanel that provides a user interface for the active part of
 * the FileSender program, including starting the sending process
 * and logging the results.
 */
public class RightPanel extends JPanel
						implements FileListener, ActionListener, SenderListener  {

	HeaderPanel headerPanel;
	JPanel centerPanel;
	FooterPanel footerPanel;
	ApplicationProperties properties;
	SourcePanel sourcePanel;
	Color background = Color.getHSBColor(0.58f, 0.17f, 0.95f);

	Sender sender = null;
	SenderScrollPane senderScrollPane;
	File currentSelection = null;

	DestinationPanel destinationPanel;

	/**
	 * Class constructor; creates an instance of the RightPanel and
	 * initializes the user interface for it.
	 * @param properties the properties object that is to be used
	 * and updated as GUI components change.
	 * @param sourcePanel the panel that contains file or directory
	 * selected for anonymization.
	 */
	public RightPanel(
			ApplicationProperties properties,
			SourcePanel sourcePanel) {
		super();
		this.properties = properties;
		this.sourcePanel = sourcePanel;
		this.setBackground(background);
		this.setLayout(new BorderLayout());
		headerPanel = new HeaderPanel();
		this.add(headerPanel,BorderLayout.NORTH);
		centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		this.add(centerPanel,BorderLayout.CENTER);
		footerPanel = new FooterPanel();
		this.add(footerPanel,BorderLayout.SOUTH);

		destinationPanel = new DestinationPanel(properties);
		centerPanel.add(destinationPanel,BorderLayout.CENTER);

		sourcePanel.getDirectoryPane().addFileListener(this);
		footerPanel.button.addActionListener(this);
	}

	/**
	 * The FileListener implementation.
	 * This method captures the current file selection when
	 * it changes in the sourcePanel.
	 * @param event the event containing the File currently selected.
	 */
	public void fileEventOccurred(FileEvent event) {
		if (event.type == FileEvent.SELECT) currentSelection = event.after;
	}

	/**
	 * The SenderListener implementation.
	 * This method logs event messages to the scroll pane
	 * and changes the action button name when the process
	 * is complete.
	 * @param event the event containing the message.
	 */
	public void fileSent(SenderEvent event) {
		if (event.done) {
			senderScrollPane.displayAll();
			int count = sender.getFileCount();
			String text = "<br><br>" + count + " file" + ((count!=1)?"s":"") + " sent";
			senderScrollPane.append(text);
			footerPanel.button.setText("Okay");
		}
		else senderScrollPane.appendErrorString(event.message);
	}

	/**
	 * The ActionListener for the Send/Cancel/Okay button.
	 * The name of the button is used to indicate the state of
	 * the process and what can happen next.
	 * @param event the event indicating what happened.
	 */
	public void actionPerformed(ActionEvent event) {
		String buttonText = footerPanel.button.getText();
		if (event.getSource() instanceof JButton) {
			if (buttonText.equals("Send")) {
				destinationPanel.destinationList.setDestinations();
				String destination = destinationPanel.destinationList.getSelectedDestination();
				boolean subdirectories = sourcePanel.getSubdirectories();
				if ((currentSelection != null) && !destination.trim().equals("")) {
					try {
						sender = new Sender(
							this,
							sourcePanel.getFileFilter(),
							currentSelection,
							subdirectories,
							footerPanel.unpackZip.isSelected(),
							footerPanel.forceMIRC.isSelected(),
							destination);
						sender.addSenderListener(this);
						footerPanel.button.setText("Cancel");
						senderScrollPane = new SenderScrollPane();
						centerPanel.removeAll();
						centerPanel.add(senderScrollPane,BorderLayout.CENTER);
						sender.start();
					}
					catch (Exception e) {
						JOptionPane.showMessageDialog(this,e.getMessage());
					}
				}
				else Toolkit.getDefaultToolkit().beep();
			}
			else if (buttonText.equals("Cancel")) {
				sender.interrupt();
				footerPanel.button.setText("Okay");
			}
			else if (buttonText.equals("Okay")) {
				String destination = destinationPanel.destinationList.getSelectedDestination();
				footerPanel.button.setText("Send");
				centerPanel.removeAll();
				destinationPanel = new DestinationPanel(properties);
				destinationPanel.destinationList.setSelectedDestination(destination);
				centerPanel.add(destinationPanel,BorderLayout.CENTER);
				destinationPanel.repaint();
			}
		}
	}

	//Class to display the heading in the proper place
	class HeaderPanel extends JPanel {
		public HeaderPanel() {
			super();
			this.setBackground(background);
			this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
			JLabel panelLabel = new JLabel(" Destination",SwingConstants.LEFT);
			Font labelFont = new Font("Dialog", Font.BOLD, 18);
			panelLabel.setFont(labelFont);
			this.add(panelLabel);
			this.add(Box.createHorizontalGlue());
			this.add(Box.createHorizontalStrut(17));
		}
	}

	//Class to display the footer with the action button and
	//the checkbox for forcing the content type.
	class FooterPanel extends JPanel implements ActionListener {
		public JButton button;
		public JCheckBox forceMIRC;
		public JCheckBox unpackZip;
		public FooterPanel() {
			super();
			this.setBackground(background);
			this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

			String mirc = (String)properties.getProperty("force-mirc");
			if (mirc == null) {
				mirc = "yes";
				properties.setProperty("force-mirc",mirc);
			}
			forceMIRC = new JCheckBox("Force MIRC Content-Type for HTTP(S)",mirc.equals("yes"));
			forceMIRC.setBackground(background);

			String unpack = (String)properties.getProperty("unpack-zip-files");
			if (unpack == null) {
				unpack = "yes";
				properties.setProperty("unpack-zip-files",unpack);
			}
			unpackZip = new JCheckBox("Unpack zip files",unpack.equals("yes"));
			unpackZip.setBackground(background);

			button = new JButton("Send");

			Box box1 = new Box(BoxLayout.X_AXIS);
			box1.add(unpackZip);
			unpackZip.addActionListener(this);
			box1.add(Box.createHorizontalGlue());
			this.add(box1);

			Box box2 = new Box(BoxLayout.X_AXIS);
			box2.add(forceMIRC);
			forceMIRC.addActionListener(this);
			box2.add(Box.createHorizontalGlue());
			box2.add(button);
			this.add(box2);
		}
		public void actionPerformed(ActionEvent evt) {
			properties.setProperty("force-mirc",(forceMIRC.isSelected() ? "yes" : "no"));
			properties.setProperty("unpack-zip-files",(unpackZip.isSelected() ? "yes" : "no"));
		}
	}

}
