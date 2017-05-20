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
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.util.*;
import java.util.zip.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;
import javax.swing.*;
import javax.swing.event.*;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.pipeline.Status;
import org.rsna.ctp.stdstages.dicom.DicomStorageSCU;
import org.rsna.ui.GeneralFileFilter;
import org.rsna.ui.SourcePanel;
import org.rsna.util.FileUtil;
import org.rsna.util.HttpUtil;

/**
 * A Thread for sending one or more files using HTTP, HTTPS, or DICOM.
 */
public class Sender extends Thread {

	private TrustManager[] trustAllCerts;

	Component parent;
	EventListenerList listenerList;
	GeneralFileFilter filter;
	File file;
	boolean subdirectories;
	boolean unpackZip;
	boolean skipDuplicates;
	boolean forceMircContentType;
	int interval;
	boolean deleteFile;
	Properties contentTypes;
	String urlString;
	String calledAET;
	String callingAET;
	String host;
	int port;
	boolean http;
	boolean https;
	boolean dicom;
	int fileCount = 0;
	int skipCount = 0;
	int timeout = 5000;
	HashSet<String> sopiUIDs;
	
	static final long maxUnchunked = 20 * 1024 * 1024;

	/**
	 * Class constructor; creating an instance of the Sender.
	 * @param parent the parent of this Thread.
	 * @param filter the file filter for selecting files to send.
	 * @param file the file to send, or if it is a directory, the
	 * directory whose files are to be sent if they match the filter.
	 * @param subdirectories true if all files in the directory and
	 * its subdirectories are to be sent; false if only files in
	 * the directory itself are to be sent; ignored if file is not
	 * a directory.
	 * @param forceMircContentType true if the HTTP content type is
	 * to be set to application/x-mirc-dicom for uploading to MIRC
	 * Clinical Trial Services; false if the file extension is to be
	 * used to determine the content type; ignored if the protocol is
	 * DICOM.
	 * @param urlString the URL of the destination.
	 */
	public Sender(Component parent,
				  GeneralFileFilter filter,
				  File file,
				  boolean subdirectories,
				  boolean unpackZip,
				  boolean skipDuplicates,
				  boolean forceMircContentType,
				  int interval,
				  boolean deleteFile,
				  String urlString) throws Exception {
		super();
		this.parent = parent;
		this.filter = filter;
		this.file = file;
		this.subdirectories = subdirectories;
		this.unpackZip = unpackZip;
		this.skipDuplicates = skipDuplicates;
		this.forceMircContentType = forceMircContentType;
		this.interval = interval;
		this.deleteFile = deleteFile;
		this.urlString = urlString;
		String urlLC = urlString.toLowerCase().trim();
		http = (urlLC.indexOf("http://") != -1);
		https = (urlLC.indexOf("https://") != -1);
		dicom = (urlLC.indexOf("dicom://") != -1);
		if (dicom) decodeUrlString();
		listenerList = new EventListenerList();
		if (!forceMircContentType) {
			try {
				InputStream is =
					parent.getClass().getResource("/content-types.properties").openStream();
				contentTypes = new Properties();
				contentTypes.load(is);
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(
					parent,
					"Unable to load the content-types.properties resource:\n" + e.getMessage());
			}
		}
	}

	/**
	 * Start the Thread.
	 */
	public void run() {
		fileCount = 0;
		skipCount = 0;
		sopiUIDs = new HashSet<String>();
		send(file);
		if (interrupted()) sendEvent("<br><b><font color=\"red\">Interrupted</font></b>",true);
		else sendEvent("<br><b>Done.</b>",true);
	}

	/**
	 * Get the number of files transmitted during the run call.
	 * @return the file count.
	 */
	public int getFileCount() {
		return fileCount;
	}

	/**
	 * Get the number of files skipped during the run call.
	 * @return the skip count.
	 */
	public int getSkipCount() {
		return skipCount;
	}

	//Unpack the URL string and make sure it is acceptable.
	private void decodeUrlString() throws Exception {
		int k = urlString.indexOf("://") + 3;
		int kk = urlString.indexOf(":",k);
		if (kk == -1) throw new Exception("Missing separator [:] for AE Titles");
		calledAET = urlString.substring(k,kk).trim();
		k = ++kk;
		kk = urlString.indexOf("@",k);
		if (kk == -1) throw new Exception("Missing terminator [@] for CallingAET");
		callingAET = urlString.substring(k,kk).trim();
		k = ++kk;
		kk = urlString.indexOf(":",k);
		if (kk == -1) throw new Exception("Missing separator [:] for Host and Port");
		host = urlString.substring(k,kk).trim();
		k = ++kk;
		String portString = urlString.substring(k).trim();
		try { port = Integer.parseInt(portString); }
		catch (Exception e) { throw new Exception("Unparseable port number ["+portString+"]"); }
	}

	// Send a file if it is not a directory.
	// If the file is a directory, send the files in the directory
	// that match the filter. If subdirectories == true, send the
	// contents of any subdirectories as well.
	private boolean send(File file) {
		if (interrupted()) return false;
		boolean result = false;

		//Handle normal files here
		if (!file.isDirectory()) {
			if (unpackZip && file.getName().endsWith(".zip")) return sendZipFile(file);
			else if (http || https) result = sendFileUsingHttp(file);
			else if (dicom) result = sendFileUsingDicom(file);
			System.gc();
			if (interval > 0) {
				try { Thread.sleep(interval); }
				catch (Exception ex) { }
			}
			if (result && deleteFile) file.delete();
			return result;
		}

		//Handle directories here
		File[] files = file.listFiles(filter);
		for (int i=0; i<files.length && !interrupted(); i++) {
			if (!files[i].isDirectory() || subdirectories) send(files[i]);
		}
		return true;
	}

	//Unpack a zip file and send all its contents.
	private boolean sendZipFile(File file) {
		if (!file.exists()) return false;
		try {
			ZipFile zipFile = new ZipFile(file);
			Enumeration zipEntries = zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry)zipEntries.nextElement();
				if (!entry.isDirectory()) {
					String name = entry.getName();
					name = name.substring(name.lastIndexOf("/")+1).trim();
					if (!name.equals("")) {
						File outFile = File.createTempFile("FS-",".tmp");
						BufferedOutputStream out =
							new BufferedOutputStream(
								new FileOutputStream(outFile));
						BufferedInputStream in =
							new BufferedInputStream(
								zipFile.getInputStream(entry));
						int size = 1024;
						int n = 0;
						byte[] b = new byte[size];
						while ((n = in.read(b,0,size)) != -1) out.write(b,0,n);
						in.close();
						out.close();
						send(outFile);
						outFile.delete();
					}
				}
			}
			zipFile.close();
			return true;
		}
		catch (Exception e) {
			sendMessage(
					"<font color=\"red\">Error unpacking and sending the zip file:<br"
					+file.getAbsolutePath() + "</font></br>"
					+ e.getMessage() + "<br>");
			return false;
		}
	}

	//Send one file using HTTP or HTTPS.
	//
	//NOTE: The error response code here tries to make a response for the user without knowing
	//anything about the receiving application, so it looks for keywords that indicate success
	//or failure. It is certainly possible to fool this code, but it works with MIRC, and
	//clearly nothing else in the world matters.
	//
	//NOTE: This code accepts all certificates when sending via HTTPS.
	private boolean sendFileUsingHttp(File file) {
		URL url;
		HttpURLConnection conn;
		BufferedOutputStream svros;
		BufferedInputStream fis;
		BufferedReader svrrdr;
		long fileLength = file.length();
		String message = "<b>" + (++fileCount) + "</b>: Send " +
						file.getAbsolutePath() + " to " + urlString + "<br>";
		try {
			url = new URL(urlString);
			conn = HttpUtil.getConnection(url);
			conn.setReadTimeout(timeout);
			conn.setConnectTimeout(timeout);

			//Set the content type
			String contentType = null;
			if (forceMircContentType) contentType = "application/x-mirc-dicom";
			else if (contentTypes != null) {
				String ext = file.getName();
				ext = ext.substring(ext.lastIndexOf(".")+1).toLowerCase();
				contentType = contentTypes.getProperty(ext);
			}
			if (contentType == null) contentType = "application/default";
			conn.setRequestProperty("Content-Type",contentType);
			if (fileLength > maxUnchunked) conn.setChunkedStreamingMode(0);

			//Set the content disposition
			conn.setRequestProperty("Content-Disposition","attachment; filename=\"" + file.getName() + "\"");
			conn.setRequestProperty("Content-Length", Long.toString(file.length()));

			//Make the connection
			conn.connect();
			svros = new BufferedOutputStream( conn.getOutputStream() );
		}
		catch (Exception e) {
			sendMessage(message +
					"<font color=\"red\">Unable to establish a URLConnection to "
					+ urlString + "</font><br>");
			e.printStackTrace();
			return false;
		}
		try {
			fis = new BufferedInputStream( new FileInputStream(file) );
		}
		catch (Exception e) {
			sendMessage(message +
					"<font color=\"red\">Unable to obtain an input stream to read the file:</font></br>"
					+ e.getMessage() + "<br>");
			return false;
		}
		//Send the file to the server
		try {
			int n;
			byte[] bbuf = new byte[1024];
			while ((n=fis.read(bbuf,0,bbuf.length)) > 0) {
				svros.write(bbuf,0,n);
			}
			svros.flush();
			//svros.close(); //do not close or response will not be received
			fis.close();
		}
		catch (Exception e) {
			sendMessage(message +
					"<font color=\"red\">Error sending the file:</font><br>"
					+ e.getMessage() + "<br>");
			return false;
		}
		try {
			int responseCode = conn.getResponseCode();
			String code = "<b>ResponseCode = "+responseCode+"</b><br>";
			if (responseCode != 200) code = "<font color=\"red\">"+code+"</font>";
			message += code;
			String response = FileUtil.getTextOrException( conn.getInputStream(), FileUtil.utf8, false );
			conn.disconnect();

			//Try to make a nice response without knowing anything about the
			//receiving application.
			String responseLC = response.toLowerCase();
			String responseFiltered = filterTagString(response);
			if (!forceMircContentType) {
				//See if we got an html page back
				if (responseLC.indexOf("<html>") != -1) {
					//We did, see if it looks like a successful submit service response
					if (responseLC.indexOf("was received and unpacked successfully") != -1) {
						//It does, just display OK
						sendMessage(message + "<b>OK</b><br><br>");
						return true;
					}
					else if ((responseLC.indexOf("unsupported") != -1) ||
							 (responseLC.indexOf("failed") != -1) ||
							 (responseLC.indexOf("error") != -1)) {
						//This looks like an error, return the whole text in red
						sendMessage(message + "<font color=\"red\">" + response + "</font><br><br>");
						return false;
					}
					else {
						//It's not clear what this is, just return the whole text in black
						sendMessage(message + "<b>" + response + "</b><br><br>");
						return false;
					}
				}
				else {
					//There's no way to know what this is, so return the whole text in black
					sendMessage(message + "<b>" + response + "</b><br><br>");
					return false;
				}
			}
			//If it was a forced MIRC content type send, then look for "error"
			else if (responseLC.indexOf("error") != -1) {
				sendMessage(message + "<font color=\"red\">" + response + "</font><br><br>");
				return false;
			}
			else {
				sendMessage(message + "<b>" + response + "</b><br><br>");
				return true;
			}
		}
		catch (Exception e) {
			sendMessage(message +
					"<font color=\"red\">Error reading the response:</font><br>"
					+ e.getMessage() + "<br><br>");
			return false;
		}
	}

	//Send one file using DICOM.
	private boolean sendFileUsingDicom(File file) {
		String sopiUID = null;
		if (skipDuplicates) {
			try {
				DicomObject dob = new DicomObject(file);
				sopiUID = dob.getSOPInstanceUID();
			}
			catch (Exception unable) { }
		}
		if (!skipDuplicates || (sopiUID == null) || !sopiUIDs.contains(sopiUID)) {
			String message = "<b>" + (++fileCount) + "</b>: Send " +
								file.getAbsolutePath() + " to " + urlString + "<br>";
			DicomStorageSCU dicomSender = new DicomStorageSCU(
													urlString,
													10000, //association timeout in ms
													true, //use new association for each file
													0, //host tag
													0, //port tag
													0, //called AET tag
													0  //calling AET tag
												);
			Status status = dicomSender.send(file);
			if (status.equals(Status.FAIL)) {
				sendMessage(message +
					"<font color=\"red\">DicomSend result = FAIL</font><br><br>");
				return false;
			}
			else if (status.equals(Status.RETRY)) {
				sendMessage(message +
					"<font color=\"red\">DicomSend result = RETRY</font><br><br>");
				return false;
			}
			else {
				sendMessage(message + "<b>OK</b><br><br>");
				if (sopiUID != null) sopiUIDs.add(sopiUID);
			}
			return true;
		}
		else {
			sendMessage((++skipCount) + ": Skip " + file.getAbsolutePath() + "<br>");
			//System.out.println(file + " skipped; skipDuplicates == "+skipDuplicates);
			return false;
		}
	}

	// Make a tag string readable as text
	private static String filterTagString(String s) {
		StringWriter sw = new StringWriter();
		char a;
		for (int i=0; i<s.length(); i++) {
			a = s.charAt(i);
			if (a == '<') sw.write(" &#60;");		//note the leading space
			else if (a == '>') sw.write("&#62; ");	//note the trailing space
			else if (a == '&') sw.write("&#38;");
			else if (a == '\"') sw.write("&#34;");
			else sw.write(a);
		}
		return sw.toString();
	}

	// The rest of this code is for handling event listeners and for sending events.

	//Send a message in a SenderEvent to the SenderListeners.
	private void sendMessage(String message) {
		fireSenderEvent(new SenderEvent(this,message));
	}

	//Send a done message in a SenderEvent to the SenderListeners.
	private void sendDone(String message) {
		fireSenderEvent(new SenderEvent(this));
	}

	//Send a message and a done flag in a SenderEvent to the SenderListeners.
	private void sendEvent(String message, boolean done) {
		fireSenderEvent(new SenderEvent(this,message,done));
	}

	/**
	* Register a SenderListener.
	* @param listener The listener to register.
	*/
	public void addSenderListener(SenderListener listener) {
		listenerList.add(SenderListener.class, listener);
	}

	/**
	* Remove a SenderListener.
	* @param listener The listener to remove.
	*/
	public void removeSenderListener(SenderListener listener) {
		listenerList.remove(SenderListener.class, listener);
	}

	/**
	* Fire a SenderEvent. The fileSent method calls are made in
	* the event dispatch thread, making it safe for GUI updates.
	*/
	private void fireSenderEvent(SenderEvent se) {
		final SenderEvent event = se;
		final EventListener[] listeners = listenerList.getListeners(SenderListener.class);
		Runnable fireEvents = new Runnable() {
			public void run() {
				for (int i=0; i<listeners.length; i++) {
					((SenderListener)listeners[i]).fileSent(event);
				}
			}
		};
		SwingUtilities.invokeLater(fireEvents);
	}

}
