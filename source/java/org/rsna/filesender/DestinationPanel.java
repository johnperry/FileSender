/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.rsna.misc.ApplicationProperties;
import org.rsna.misc.PairedLayout;

/**
 * The JPanel providing a user interface for entering and
 * selecting destination URLs for the FileSender application
 */
public class DestinationPanel extends JPanel {

	ApplicationProperties properties;
	DestinationList destinationList;
	Color background = Color.getHSBColor(0.58f, 0.17f, 0.95f);

	/**
	 * Class constructor; creates a new instance of the DestinationPanel
	 * and initializes it from the application's properties.
	 * @param properties the Properties object containing all the
	 * previously defined destination URLs and the checkbox states.
	 */
	public DestinationPanel(ApplicationProperties properties) {
		super();
		this.properties = properties;
		this.setLayout(new BorderLayout());
		this.setBackground(background);
		this.add(new InstructionPanel(),BorderLayout.NORTH);
		destinationList = new DestinationList();
		this.add(destinationList,BorderLayout.CENTER);
	}

	//Class to put the heading on the main body of the JPanel. This
	//heading provides example URLs to make it easy to remember
	//all the URL fields.
	private class InstructionPanel extends Box {
		public InstructionPanel() {
			super(BoxLayout.Y_AXIS);
			JLabel label1 = new JLabel("Select a destination or add a new one.");
			JLabel label2 = new JLabel("[http://host:port/path]");
			JLabel label3 = new JLabel("[https://host:port/path]");
			JLabel label4 = new JLabel("[dicom://destinationAET:senderAET@host:port]");
			JLabel label5 = new JLabel("[http://host:port/submit/sss{n}?ppt]");
			Font font = new Font("Courier New", Font.PLAIN, 12);
			label2.setFont(font);
			label3.setFont(font);
			label4.setFont(font);
			label5.setFont(font);
			Box box1 = Box.createHorizontalBox();
			box1.add(label1);
			this.add(box1);
			Box box2 = Box.createHorizontalBox();
			box2.add(label2);
			this.add(box2);
			Box box3 = Box.createHorizontalBox();
			box3.add(label3);
			this.add(box3);
			Box box4 = Box.createHorizontalBox();
			box4.add(label4);
			this.add(box4);
			Box box5 = Box.createHorizontalBox();
			box5.add(label5);
			this.add(box5);
		}
	}

	//Class to contain a scrollable set of editable
	//and selectable URL fields.
	class DestinationList extends JScrollPane {

		String[] destinations;
		JTextField[] fields;
		JRadioButton[] buttons;
		ButtonGroup group;

		public DestinationList() {
			super();
			this.getVerticalScrollBar().setUnitIncrement(25);
			Font inputFont = new Font("Courier New", Font.PLAIN, 12);
			SpecialScrollablePanel panel = new SpecialScrollablePanel();
			destinations = getDestinations();
			fields = new JTextField[destinations.length + 1];
			buttons = new JRadioButton[destinations.length + 1];
			group = new ButtonGroup();
			for (int i=0; i<destinations.length + 1; i++) {
				buttons[i] = new JRadioButton();
				group.add(buttons[i]);
				if (i == destinations.length)
					fields[i] = new JTextField("",50);
				else
					fields[i] = new JTextField(destinations[i],50);
				fields[i].setFont(inputFont);
				panel.add(buttons[i]);
				panel.add(fields[i]);
			}
			this.setViewportView(panel);
		}

		private String[] getDestinations() {
			String dest;
			LinkedList<String> list = new LinkedList<String>();
			for (int i=0; (dest=properties.getProperty("destination["+i+"]"))!=null; i++)
				list.add(dest);
			return list.toArray(new String[list.size()]);
		}

		public void setDestinations() {
			String dest;
			int field = 0;
			for (int i=0; i<fields.length; i++) {
				dest = fields[i].getText().trim();
				if (!dest.equals("")) {
					properties.setProperty("destination["+field+"]",dest);
					field++;
				}
			}
			properties.remove("destination["+field+"]");
		}

		public String getSelectedDestination() {
			for (int i=0; i<buttons.length; i++) {
				if (buttons[i].isSelected()) return fields[i].getText().trim();
			}
			return "";
		}

		public void setSelectedDestination(String destination) {
			for (int i=0; i<buttons.length; i++) {
				if (fields[i].getText().equals(destination)) {
					buttons[i].setSelected(true);
					return;
				}
			}
		}
	}

    class SpecialScrollablePanel extends JPanel implements Scrollable {
		public SpecialScrollablePanel() {
			super();
			this.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			this.setLayout(new PairedLayout(0,5));
		}
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			if (orientation == SwingConstants.HORIZONTAL) return visibleRect.width/2;
			return visibleRect.height/2;
		}
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 12;
		}
	}
}



