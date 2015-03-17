/* 
 * This class was created to handle the accepting of new connections. 
 * When this class receives and accepts a new connection a  
 * ProcessConnectionThread class instance is created. The ProcessConnectionThread
 * thread instance will then handle any requests sent to it.
 * This class essentially accepts and then hands the duties of processing to 
 * another thread.
 * 
 * There should only ever be 1 instance of this thread! 
 * 
 * This class was created by Denise Blady, known as DB in comments,
 * for the SEASAME project. Inspiration for this class's design came from
 * a tutorial online showing how to use bluecove to create a bluetooth server.
 * Additionally the tutorial was improved upon using another bluecove example,
 * shown as the second link.
 * Links: 
 *   http://luugiathuy.com/2011/02/android-java-bluetooth/
 *   http://fivedots.coe.psu.ac.th/~ad/jg/blue4/blueCoveEcho.pdf
 */

package com.example.sesameserver;

import java.io.IOException;
import java.util.ArrayList;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class WaitThread extends Thread implements Runnable{
	//Unsure why isRunning is 'volatile' but eclipse insisted. -DB
	//isRunning controls whether you are in or out of the main while loop!
	private volatile boolean isRunning = false;
	private ArrayList<ProcessConnectionThread> handlers;
	private StreamConnectionNotifier notifier;
	
	/** Constructor */
	public WaitThread() {
		handlers = new ArrayList<ProcessConnectionThread>();

		//When program gets shutdown via GUI, this says to call closeDown()
	    Runtime.getRuntime().addShutdownHook(new Thread() {
	      public void run() 
	      {  closeDown(); }
	    });
	}
	
	@Override
	public void run() {
		waitForConnection();
		System.out.println("Exiting WaitThread");
	}
	
	/** Waiting for connection from devices */
	private void waitForConnection() {
		
		// retrieve the local Bluetooth device object
		// and setup the server to listen for connections
		LocalDevice local = null;
		
		try {
			local = LocalDevice.getLocalDevice();
			local.setDiscoverable(DiscoveryAgent.GIAC);
			
			UUID uuid = new UUID("191a372061cb11e2bcfd0800200c9a66", false);
			System.out.println(uuid.toString());
			
			//Advertise Server
            String url = "btspp://localhost:" + uuid.toString() + ";name=RemoteLoginBT";
            notifier = (StreamConnectionNotifier)Connector.open(url);
        } catch (BluetoothStateException e) {
        	System.out.println("Bluetooth is not turned on.");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// waiting for connection
		isRunning = true;
		while(isRunning) {
			try {
				System.out.println("waiting for connection...");
				RemoteLoginServer.myGUI.AddTexttoWindow("waiting for connection...\n");
	            StreamConnection connection = notifier.acceptAndOpen();
	            System.out.println("Accepted new connection");
				RemoteLoginServer.myGUI.AddTexttoWindow("Accepted new connection\n");
	            
	            //Let processThread handle client requests
	            ProcessConnectionThread processThread = new ProcessConnectionThread(connection);
	            handlers.add(processThread);
	            processThread.start();
	            
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} //End while loop
	}//End waitForConnection()
	
	  /** Stop accepting any further client connections, and close down
	     all the processing handlers. */
	  public void closeDown() {
	    System.out.println("Closing down server");
	    if (isRunning) {
	      isRunning = false;
	      try {
	    	// close connection, and remove service record from SDDB  
	        notifier.close(); 
	      }
	      catch (IOException e) 
	      {  System.out.println(e);  }

	      // close down all the handlers
	      for (ProcessConnectionThread hand : handlers)
	         hand.closeDown();
	      
	      handlers.clear();
	    }
	  }  // end of closeDown()
	  
}