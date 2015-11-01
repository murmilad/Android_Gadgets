package info.murmilad.pebble.gadgets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import static info.murmilad.pebble.gadgets.Constants.*;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class GadgetService extends Service {

    private static final String TAG = "GadgetsService";
	private PebbleKit.PebbleDataReceiver mReceiver;
	private Handler mHandler = new Handler();

	static SharedPreferences settings;

    private boolean isRunning  = false;

    private LinkedList<PebbleDictionary> gadgets = new LinkedList<PebbleDictionary>();
    private LinkedList<PebbleDictionary> figures = new LinkedList<PebbleDictionary>();

    private BluetoothAdapter btAdapter;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
   
    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");


        isRunning = true;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
    	
    	Log.i(TAG, "Service onStartCommand");

        boolean isConnected = PebbleKit.isWatchConnected(this);
        Toast.makeText(this, "Pebble " + (isConnected ? "is" : "is not") + " connected!", Toast.LENGTH_LONG).show();

        // Get information back from the watchapp
        if(mReceiver == null) {
        	
        	mReceiver = new PebbleKit.PebbleDataReceiver(Constants.GADGETS_UUID) {
	            @Override
	            public void receiveData(Context context, int id, PebbleDictionary data) {
	              // Always ACKnowledge the last message to prevent timeouts
	              PebbleKit.sendAckToPebble(getApplicationContext(), id);
	
	              // Get action and display
	              Command command = Command.valueOf(data.getString(Constants.KEY_GADGET_COMMAND));

	              switch (command) {
					case send_gadgets:
						Log.i(TAG, "send_gadgets");

						PebbleDictionary gadget = new PebbleDictionary();
				    	gadget.addString(KEY_GADGET_NAME, "CO2");
				    	gadget.addUint8(KEY_GADGET_NUMBER, (byte) 0);
				    	
				    	gadgets.add(gadget);
				    	
				    	gadget = new PebbleDictionary();
				    	gadget.addString(KEY_GADGET_NAME, "Temper.");
				    	gadget.addUint8(KEY_GADGET_NUMBER,  (byte) 1);

				    	gadgets.add(gadget);

				    	gadget = new PebbleDictionary();
				    	gadget.addString(KEY_GADGET_NAME, "Humidity");
				    	gadget.addUint8(KEY_GADGET_NUMBER,  (byte) 2);

				    	gadgets.add(gadget);

						if (gadgets.size() > 0) {
					        PebbleDictionary outgoing = new PebbleDictionary();
					        outgoing.addUint8(KEY_GADGET_COUNT, (byte) gadgets.size());
					        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);
						} else {
					        PebbleDictionary outgoing = new PebbleDictionary();
					        outgoing.addUint8(KEY_FIGURES_SENT, (byte) 1);
					        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);
						}
							
						break;

					case send_next_gadget:
						if (gadgets.size() > 0) {
							PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, gadgets.pop());
						} else {
					        PebbleDictionary outgoing = new PebbleDictionary();
					        outgoing.addUint8(KEY_FIGURES_SENT, (byte) 1);
					        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);
						}
							
						break;
	
					case send_figures:
						Log.i("Gadgets", "Bluetooth get figures");
					    final Bluetooth bluetooth = new Bluetooth(intent.getStringExtra("deviceAddress"));

					    bluetooth.setBluetoothIn(new Handler() {
					        public void handleMessage(android.os.Message msg) {
					            if (msg.what == 0) {                                     //if message is what we want
					                String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
					                recDataString.append(readMessage);                                      //keep appending to string until ~
					                int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
					                if (endOfLineIndex > 0) {                                           // make sure there data before ~
					                    String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
					                    Log.i("Gadgets", "Bluetooth Info: " + dataInPrint);
					                    Pattern gadgetPattern = Pattern.compile("([^,]+),([^,]+),([^,]+),([^\\r\\n]+)");
					                    Matcher gadgetMatcher = gadgetPattern.matcher(dataInPrint);
					                    if (gadgetMatcher.find()) {
					                    	PebbleDictionary figure = new PebbleDictionary();
					                    	figure.addInt16(KEY_GADGET_VALUE, (short) 0);
					                    	figure.addUint8(KEY_GADGET_TYPE, (byte) 0);
					                    	figure.addString(KEY_GADGET_STRING, gadgetMatcher.group(4));
					                    	figures.add(figure);
					                    	
					                    	figure = new PebbleDictionary();
					                    	figure.addInt16(KEY_GADGET_VALUE, (short) 253);
					                    	figure.addUint8(KEY_GADGET_TYPE,  (byte) 1);
					                    	figure.addString(KEY_GADGET_STRING, gadgetMatcher.group(1));
					                    	figures.add(figure);

					                    	figure = new PebbleDictionary();
					                    	figure.addInt16(KEY_GADGET_VALUE, (short) 253);
					                    	figure.addUint8(KEY_GADGET_TYPE,  (byte) 2);
					                    	figure.addString(KEY_GADGET_STRING, gadgetMatcher.group(2));
					                    	figures.add(figure);

											if (figures.size() > 0) {
										        PebbleDictionary outgoing = new PebbleDictionary();
										        outgoing.addUint8(KEY_FIGURE_COUNT, (byte) figures.size());
										        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);
											} else {
										        PebbleDictionary outgoing = new PebbleDictionary();
										        outgoing.addUint8(KEY_FIGURES_SENT, (byte) 2);
										        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);
											}
											bluetooth.disconnect();

					                    }
					                    recDataString.delete(0, recDataString.length());                    //clear all string data
					                }
					            }
					        }
					    });
					    bluetooth.run();
						 
							
						break;

					case send_next_figure:
						if (figures.size() > 0) {
							PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, figures.pop());
						} else {
					        PebbleDictionary outgoing = new PebbleDictionary();
					        outgoing.addUint8(KEY_FIGURES_SENT, (byte) 2);
					        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);
						}
							
						break;
					default:
						break;
					}
	            }
          };
        }
        
     // Register the receiver to get data
        PebbleKit.registerReceivedDataHandler(this, mReceiver);

//        PebbleDictionary outgoing = new PebbleDictionary();
//        outgoing.addUint8(KEY_GADGET_COUNT, (byte) gadgets.size());
//        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.GADGETS_UUID, outgoing);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//            	
//
//                for (int i = 0; i < 5; i++) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (Exception e) {
//                    }
//
//                    if(isRunning){
//                        Log.i(TAG, "Service running");
//                    }
//                }
//
//                stopSelf();
//            }
//        }).start();

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {

    	unregisterReceiver(mReceiver);
    	
        isRunning = false;
        Log.i(TAG, "Service onDestroy");
    }
}
