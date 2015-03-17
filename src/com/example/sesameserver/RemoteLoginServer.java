/*
 * This class was created to start the server and pop up a GUI for the user
 * so that the server could be safely shutdown and cleaned up appropriately.
 * In future the GUI may be expanded to include functionality such as:
 *      -Resetting the server's keys, but not showing the new value
 *      -Controlling the Leash distance (far vs. short)
 *      -Restart server button, but I believe its not needed right now.
 * 
 * This class starts the server by starting the WaitThread class instance. 
 * The WaitThread Thread Instance handles accepting connections.
 * 
 * This class was created by Denise Blady, known as DB in comments,
 * for the SEASAME project. Inspiration for this class's design came from
 * a tutorial online showing how to use bluecove to create a bluetooth server.
 * Link:
 *   http://luugiathuy.com/2011/02/android-java-bluetooth/
 *   http://fivedots.coe.psu.ac.th/~ad/jg/blue4/blueCoveEcho.pdf
 */

package com.example.sesameserver;


import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openqa.selenium.remote.html5.AddWebStorage;

import java.awt.*;
import java.awt.event.*;

public class RemoteLoginServer extends Frame implements WindowListener,
		ActionListener {

	// Unsure why I needed to add the following line, but eclipse insisted. -DB
	private static final long serialVersionUID = 6140193360468661890L;
	static JTextArea text = new JTextArea(10,30);
	Button bExitServer;
	static WaitThread waitThread = null;
	static RemoteLoginServer myGUI;
	
	public static void main(String[] args) {
		// display local device address and name
		LocalDevice localDevice;
		
		myGUI = new RemoteLoginServer(
				"SEASAME Server Controller");
		myGUI.setSize(400, 200);
		myGUI.setVisible(true);
		
		try {
			localDevice = LocalDevice.getLocalDevice();
			System.out.println("Address: " + localDevice.getBluetoothAddress());
			System.out.println("Name: " + localDevice.getFriendlyName());
		} catch (BluetoothStateException e) {
			e.printStackTrace();
			myGUI.AddTexttoWindow("Error with Bluetooth Connection." +
					"\nMake Sure Bluetooth is ON and you are connected to " +
					"SeSameClient");
		}

		// start server
		waitThread = new WaitThread();
		waitThread.start();
		
	}

	/**
	 * Constructor to set up GUI elements of Server Controller
	 * 
	 * @param title
	 *            the title of the window
	 * */
	public RemoteLoginServer(String title) {
		super(title);
		setLayout(new FlowLayout());
		addWindowListener(this);
		bExitServer = new Button("Close Server");
		add(bExitServer,0);
		add(new JScrollPane(text));
		bExitServer.addActionListener(this);
	}

	public void AddTexttoWindow(String Text){
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.append(Text);
	}
	
	/*
	 * When bExitServer is clicked, close down server threads and GUI. Closing
	 * the waitThread will begin a cascade of closing threads, see WaitThread
	 * class for more information
	 */
	public void actionPerformed(ActionEvent e) {
		waitThread.closeDown();
		System.out.println("RemoteLoginServer closed");
		dispose();
		System.exit(0);
	}

	/*
	 * Same as above method. I don't need to explicitly call closeDown() on the
	 * waitThread because of line 41 in the WaitThread class. It adds a shutdown
	 * hook so that when this method is called closeDown() is called by the
	 * shutdown hook. -DB
	 */
	public void windowClosing(WindowEvent e) {
		System.out.println("RemoteLoginServer closed");
		dispose();
		System.exit(0);
	}

	// Does nothing so far, to my knowledge. It is required for implementation
	// of WindowListener. -DB
	public void windowOpened(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

}
