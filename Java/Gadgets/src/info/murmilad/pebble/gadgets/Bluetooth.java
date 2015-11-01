package info.murmilad.pebble.gadgets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class Bluetooth {

	 
	  Handler bluetoothIn;
	 
	  public Handler getBluetoothIn() {
		return bluetoothIn;
	}


	public void setBluetoothIn(Handler bluetoothIn) {
		this.bluetoothIn = bluetoothIn;
	}

	final int handlerState = 0;                        //used to identify handler message
	  private BluetoothAdapter btAdapter = null;
	  private BluetoothSocket btSocket = null;
	  private StringBuilder recDataString = new StringBuilder();
	  private ConnectedThread mConnectedThread;
	 
	 
	  // SPP UUID service - this should work for most devices
	  private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	 
	  // String for MAC address
	  private static String address;
	 
	  Bluetooth(String deviceAddress) {
	 
		address = deviceAddress;
	 
	    btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
	  }
	  
	  
	  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
	 
	      return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
	      //creates secure outgoing connecetion with BT device using UUID
	  }
	 
	  public void run() {
	 
	 
	    //create device and set the MAC address
	    BluetoothDevice device = btAdapter.getRemoteDevice(address);
	 
	    try {
	        btSocket = createBluetoothSocket(device);
	    } catch (IOException e) {
	        Log.e("Gadgets", "Socket creation failed");
	    }
	    // Establish the Bluetooth socket connection.
	    try
	    {
	      btSocket.connect();
	    } catch (IOException e) {
	      try
	      {
	        btSocket.close();
	        Log.e("Gadgets", "Connect failed");
	      } catch (IOException e2)
	      {
            Log.e("Gadgets", "Connect failed 2");
	        //insert code to deal with this
	      }
	    }
	    mConnectedThread = new ConnectedThread(btSocket);
	    mConnectedThread.start();
	 
	    //I send a character when resuming.beginning transmission to check device is connected
	    //If it is not an exception will be thrown in the write method and finish() will be called
	    mConnectedThread.write("0");
	    try {
	        Thread.sleep(5000);
	    } catch(InterruptedException ex) {
	        Thread.currentThread().interrupt();
	    }
	    mConnectedThread.read();
	  }
	 
	  public void disconnect()
	  {
	    try
	    {
	    //Don't leave Bluetooth sockets open when leaving activity
	      btSocket.close();
	    } catch (IOException e2) {
	        //insert code to deal with this
	    }
	  }

	  //create new class for connect thread
	  private class ConnectedThread extends Thread {
	        private final InputStream mmInStream;
	        private final OutputStream mmOutStream;
	 
	        //creation of the connect thread
	        public ConnectedThread(BluetoothSocket socket) {
	            InputStream tmpIn = null;
	            OutputStream tmpOut = null;
	 
	            try {
	                //Create I/O streams for connection
	                tmpIn = socket.getInputStream();
	                tmpOut = socket.getOutputStream();
	            } catch (IOException e) { }
	 
	            mmInStream = tmpIn;
	            mmOutStream = tmpOut;
	        }
	 
	        public void read() {
	            byte[] buffer = new byte[256];
	            int bytes; 
	 
	            // Keep looping to listen for received messages
	            while (true) {
	                try {
	                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
	                    String readMessage = new String(buffer, 0, bytes);
	                    // Send the obtained bytes to the UI Activity via handler
	                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
	                    break;
	                } catch (IOException e) {
	                    break;
	                }
	            }
	        }
	        //write method
	        public void write(String input) {
	            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
	            try {
	                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
	            } catch (IOException e) {
	                //if you cannot write, close the application
	                Log.e("Gadgets","Connection Failure");
	 
	              }
	        }
	    }
 }

