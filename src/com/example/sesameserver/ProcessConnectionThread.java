/*
 * This class handles the client requests/messages sent to the server. They
 * are handled in their thread, each client has their own. This class changes,
 * as needed, when new commands must be processed.
 * 
 * Each client has its own ProcessConnectionThread!
 * No personal leash setting per client because you could set it such
 * that its always below the accepted value, thereby using a DoS attack 
 * on the application. Therefore the leash is hard set and not controlled
 * by the user.
 * 
 * Needs to be tested with multiple clients at the same time still! TODO
 * 
 * This class was created by Denise Blady, known as DB in comments,
 * for the SEASAME project. Inspiration for this class's design came from
 * a tutorial online showing how to use bluecove to create a bluetooth server.
 * Additionally the tutorial was improved upon using another bluecove example,
 * shown as the second link.
 * Links: 
 *   http://luugiathuy.com/2011/02/android-java-bluetooth/
 *   http://fivedots.coe.psu.ac.th/~ad/jg/blue4/blueCoveEcho.pdf
 *   
 * Used for Signal Strength example in bluecove 2.1.0  
 *   http://www.java2s.com/Open-Source/Java/Development/bluecove-2.1.0/net/sf/bluecove/awt/ClientConnectionDialog.java.htm
 * 
 * Useful for using Selenium:
 *   http://code.google.com/p/selenium/wiki/NextSteps
 */

package com.example.sesameserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import com.intel.bluetooth.RemoteDeviceHelper;


public class ProcessConnectionThread extends Thread implements Runnable {

	private StreamConnection mConnection = null; // client connection
	private InputStream inputStream = null;
	private OutputStream outputStream=null;
	// not sure why it needs to be volatile. -DB
	// controls main while loop!
	private volatile boolean isRunning = false;
	private int signalStr = -6000; // default says close connection b/c weak
	// signal
	private int acceptStr = -5; // signal strength at which we accept a command.

	private void brokenLeash() {
		closeDown();
		if (mConnection != null) {
			try {
				inputStream.close();
				mConnection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // end if
	} // end actionPerformed()

	// Constant that indicate command from devices
	public static final int PING_SERVER = 3; // used to constantly measure RSSI
	public static final int LOGIN_BT = 1;
	public static final int EXIT_CMD = -1;
	static final int BACKUP_OK = 7;
	static final int BACKUP=5;
	static final int RESTORE=6;
	private int FILE_SIZE=10000000;
	String FileName="sesame.db";

	/** Constructor */
	public ProcessConnectionThread(StreamConnection connection) {
		mConnection = connection;
	}

	@Override
	public void run() {
		try {
			
			// start GUI to gracefully exit server when needed
			
			// prepare to receive data
			inputStream = mConnection.openInputStream();
			outputStream = mConnection.openOutputStream();
			System.out.println("  waiting for input");
			RemoteLoginServer.myGUI.AddTexttoWindow("waiting for input\n");
			
			isRunning = true;
			while (isRunning) {
				// Used to check for any thread interruptions -DB
				byte[] bytemsg = new byte[1024];
				int numRead = inputStream.read(bytemsg);
				if (numRead > 0) { // msg received, so read it
					// check signal strength of remote device
					RemoteDevice myDevice = RemoteDeviceHelper
							.implGetRemoteDevice(mConnection);
					try {
						String signalStrString = LocalDevice
								.getProperty("bluecove.nativeFunction:getRemoteDeviceRSSI:"
										+ myDevice.getBluetoothAddress());
						signalStr = Integer.parseInt(signalStrString);
						System.out.println("RSSI: " + signalStr);
						if (signalStr < acceptStr) {
							// Signal strength too low, close connection
							brokenLeash();
							break;
						}
					} catch (Exception e) {
						System.out.println("Couldn't get Signal Strength. "
								+ e.getMessage());
					}

					// Signal strength ok, process
					System.out.println("  Read " + numRead + " bytes");
					String msg = new String(bytemsg, "ISO-8859-1");
					System.out.println("  got message: " + msg);
					int exit = msg.indexOf("-1");
					int ping = msg.indexOf("3");
					System.out.println("  index of exit cmd: " + exit);
					int command = 0; // null value, corresponds to no command
					if (exit == 0) { // '-1' found at beginning
						command = EXIT_CMD;
					} else if (ping == 0) {
						command = PING_SERVER;
					} else {
						// no occurence of exit cmd. do nothing
						System.out
						.println("  Not an exit command, process it.");
					}

					if (command == EXIT_CMD) {
						System.out.println("  finish process");
						isRunning = false;
						break;
					} else if (command == PING_SERVER) {
						System.out.println("  ping!");
					} else {
						processCommand(msg); // process the message
					}

				} else { // no messages were received, so close connection
					isRunning = false;
					System.out.println("  got message: " + numRead);
				}
			} // end while loop

			// cleanup time
			if (mConnection != null) {
				inputStream.close();
				mConnection.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		//		System.out.println("  Exiting ProcessConnectionThread");
	} // End of run()

	/** Close down ProcessConnectionThread */
	public void closeDown() {
		isRunning = false;
	}

	/**
	 * Process the message from client
	 * 
	 * @param message
	 *            the command message
	 */
	private void processCommand(String message) {
		try {
			isRunning = true; // still running
			System.out.println("  processing cmd");
			RemoteLoginServer.myGUI.AddTexttoWindow("processing cmd\n");
			// Process it
			int indexLogin = message.indexOf("1");

			int Backup = message.indexOf("5");

			int Restore = message.indexOf("6");

			System.out.println("  index of login cmd: " + indexLogin);

			int command = 0; // null value, not a command
			if (indexLogin == 0) { // '1' found at beginning
				command = LOGIN_BT;
			}
			else if (Backup == 0 ){
				command=BACKUP;
			}
			else if (Restore == 0 ){
				command=RESTORE;
			}
			else {
				System.out.println("  Not an actual command, ignoring");
				
			}

			// add other command cases when needed

			// handle the command
			switch (command) {

			case BACKUP:
				int bytesRead;
				int current = 0;
				FileOutputStream fos = null;
				BufferedOutputStream bos = null;
				String []Split= message.split(" ");

				try {
					//find out total file size to receive- sent by client in the initial message

					int total=Integer.parseInt(Split[1].trim());

					System.out.println("Connecting..."+total);

					// receive file
					byte [] mybytearray  = new byte [FILE_SIZE];

					fos = new FileOutputStream(FileName);
					bos = new BufferedOutputStream(fos);
					bytesRead = inputStream.read(mybytearray,0,mybytearray.length);
					current = bytesRead;

					do {
						bytesRead =
								inputStream.read(mybytearray, current, (mybytearray.length-current));

						if(bytesRead >= 0) current += bytesRead;
					} while(current<total);

					bos.write(mybytearray, 0 , current);
					bos.flush();
					System.out.println("File " + FileName
							+ " downloaded (" + current + " bytes read)");
					RemoteLoginServer.myGUI.AddTexttoWindow("File " + FileName
							+ " downloaded (" + current + " bytes read)\n");

					String AckString=""+BACKUP_OK;
					outputStream.write(AckString.getBytes());
					outputStream.flush();
				}
				
				finally {
					if (fos != null) fos.close();
					if (bos != null) bos.close();
				}

				break;

				//Restore - send Db file to client
			case RESTORE:
				File myFile = new File (FileName);

				//Restore message to send to client including file size.
				final String messagesend = RESTORE + " "
						+myFile.length()+" ";

				FileInputStream fis = null;
				BufferedInputStream bis = null;
				System.out.println(messagesend);
				try {

					Charset ISO = Charset.forName("ISO-8859-1");
					byte[] bytes = messagesend.getBytes(ISO);
					outputStream.write(bytes);	
					outputStream.flush();

					byte [] mybytearray  = new byte [(int)myFile.length()];
					fis = new FileInputStream(myFile);
					bis = new BufferedInputStream(fis);
					bis.read(mybytearray,0,mybytearray.length);

					System.out.println("\nSending " + FileName + "(" + mybytearray.length + " bytes)");
					RemoteLoginServer.myGUI.AddTexttoWindow("Sending " + FileName + "(" +
					mybytearray.length + " bytes)\n");
					outputStream.write(mybytearray,0,mybytearray.length);
					outputStream.flush();
					System.out.println("Done.");
				}
				finally {
					if (bis != null) bis.close();
				}
				break;


			case LOGIN_BT:
				int indexGmail = message.indexOf("gmail");
				int indexubmail = message.indexOf("ubmail");
				int indexFacebook = message.indexOf("facebook");
				int indexTwitter = message.indexOf("twitter");
				int indexQQ = message.indexOf("qq");
				System.out.println("Gmail: " + indexGmail + " ubmail: "
						+ indexubmail + "Facebook: " + indexFacebook
						+ " Twitter: " + indexTwitter + " QQ: " + indexQQ);
				String[] info = message.split("\\^\\|\\^");
				String emailField = "";
				String pswdField = "";
				if (info.length == 3) { // proper string, grab stuff
					emailField = info[1];
					pswdField = info[2];
				}
				// Without paring first space a space seems to be have added...
				emailField = emailField.substring(1);
				pswdField = pswdField.substring(1);

				// Start Firefox driver
				WebDriver driver = new FirefoxDriver();

				if (indexGmail == 2) {
					driver.get("https:mail.google.com");
					WebElement emailId = driver.findElement(By.id("Email"));
					emailId.sendKeys(emailField);
					WebElement passwId = driver.findElement(By.id("Passwd"));
					passwId.sendKeys(pswdField);
					// Send it in, after looking at page source you can find id
					// of sign in button
					WebElement signIn = driver.findElement(By.id("signIn"));
					signIn.click();

				} else if (indexubmail == 2) {
					driver.get("https://ubmail.buffalo.edu");
					WebElement emailId = driver.findElement(By.id("login"));
					emailId.sendKeys(emailField.trim());
					WebElement passwId = driver.findElement(By.id("password"));
					System.out.println(emailField+":"+pswdField);
					passwId.sendKeys(pswdField.trim());
					// Send it in, after looking at page source you can find id
					// of sign in button
					WebElement signIn = driver.findElement(By.id("login-button"));
					signIn.click();
				}

				else if (indexFacebook == 2) {
					driver.get("https:www.facebook.com");
					WebElement emailId = driver.findElement(By.id("email"));
					emailId.sendKeys(emailField);
					WebElement passwId = driver.findElement(By.id("pass"));
					passwId.sendKeys(pswdField);
					// Send it in, after looking at page source you can find id
					// of sign in button
					WebElement signIn = driver
							.findElement(By.id("loginbutton"));
					signIn.click();
				}

				else if (indexTwitter == 2) {
					driver.get("https://mobile.twitter.com/session/new");
					WebElement emailId = driver.findElement(By.id("username"));
					emailId.sendKeys(emailField);
					WebElement passwId = driver.findElement(By.id("password"));
					passwId.sendKeys(pswdField);
					// Send it in, after looking at page source you can find id
					// of sign in button
					// WebElement
					// signIn=driver.findElement(By.className("submit btn primary-btn flex-table-btn js-submit"));
					WebElement signIn = driver.findElement(By
							.id("signupbutton"));
					signIn.click();
				} else if (indexQQ == 2) {
					driver.get("https:en.mail.qq.com");
					WebElement emailId = driver.findElement(By.id("uin"));
					emailId.sendKeys(emailField);
					WebElement passwId = driver.findElement(By.id("p"));
					passwId.sendKeys(pswdField);
					// Send it in, after looking at page source you can find id
					// of sign in button
					// WebElement
					// signIn=driver.findElement(By.className("submit btn primary-btn flex-table-btn js-submit"));
					WebElement signIn = driver.findElement(By.id("btlogin"));
					signIn.click();
				}
				// end If statements
				break;

			default:
				System.out.println("  Not a recognizable cmd, ignoring: "
						+ message);
				break;
			}
			System.out.println("  Done processing cmd");
			RemoteLoginServer.myGUI.AddTexttoWindow("Done processing cmd\n");
		} catch (Exception e) {
			e.printStackTrace();
			RemoteLoginServer.myGUI.AddTexttoWindow(e.getLocalizedMessage()+"\n");
		}
	} // end processCommand()

}
