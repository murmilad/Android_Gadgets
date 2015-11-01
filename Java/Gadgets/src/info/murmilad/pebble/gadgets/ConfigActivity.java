package info.murmilad.pebble.gadgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.getpebble.android.kit.PebbleKit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


public class ConfigActivity extends Activity {

	static SharedPreferences settings;
	static SharedPreferences.Editor editor;

	private String deviceAddress;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	settings = this.getPreferences(MODE_WORLD_WRITEABLE);
    	editor = settings.edit();
		setDeviceAddress(settings.getString("deviceAddress", "0"));

        setContentView(R.layout.activity_config);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        List<String> deviceNames = new ArrayList<String>();
        final List<String> deviceValues = new ArrayList<String>();
        Integer currentDevice = null;
        for(BluetoothDevice bt : pairedDevices) {
        	deviceNames.add(bt.getName());
        	deviceValues.add(bt.getAddress());
        	if (getDeviceAddress().equals(bt.getAddress()))
        		currentDevice = deviceValues.size() - 1;
        }

        Spinner spinner = (Spinner) findViewById(R.id.bluetooth_device);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (currentDevice != null) {
        	spinner.setSelection(currentDevice);
        }

        spinner.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				editor.putString("deviceAddress", deviceValues.get(position));
				editor.commit();
				setDeviceAddress(deviceValues.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
        	
        });
        
        final Button buttonApply = (Button) findViewById(R.id.button_apply);
        buttonApply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent intent = new Intent(ConfigActivity.this, GadgetService.class);
            	intent.putExtra("deviceAddress", getDeviceAddress());
            	stopService(intent);
            	startService(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
      super.onResume();

    }

	public String getDeviceAddress() {
		return deviceAddress;
	}

	public void setDeviceAddress(String deviceAddress) {
		this.deviceAddress = deviceAddress;
	}

}
