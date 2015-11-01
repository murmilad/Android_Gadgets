package info.murmilad.pebble.gadgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GadgetBootUpReceiver extends  BroadcastReceiver {

	   @Override
	    public void onReceive(Context context, Intent intent) {
	            /****** For Start Activity *****/
	            Intent i = new Intent(context, ConfigActivity.class);  
	            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            context.startActivity(i);  

	           /***** For start Service  ****/
	            Intent myIntent = new Intent(context, GadgetService.class);
	      context.startService(myIntent);
	    }   

}
