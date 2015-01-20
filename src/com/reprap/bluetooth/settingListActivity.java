package com.reprap.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import java.util.Set;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.util.List;
import java.util.Map;
import android.widget.AdapterView;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link settingDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link settingListFragment} and the item details
 * (if present) is a {@link settingDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link settingListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class settingListActivity extends FragmentActivity
        implements settingListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private BluetoothAdapter _bluetoothAdapter;
    private static int REQUEST_ENABLE_BT = 1;
    private ListView _blueList;
    private ArrayAdapter<String> _arrayAdapter;
    private java.lang.Thread _stopThread;
    private boolean _stop = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blue_enum_interface);

        if (findViewById(R.id.setting_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((settingListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.setting_list))
                    .setActivateOnItemClick(true);
        }
        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if( _bluetoothAdapter == null )
        {
        	// Device does not support Bluetooth
        	return;
        }
        if( !_bluetoothAdapter.isEnabled() )
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);        	
        }
        Set<BluetoothDevice> pairedDevices =_bluetoothAdapter.getBondedDevices();
        _blueList = (ListView)findViewById(R.id.listView1);

        _arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        _blueList.setAdapter(_arrayAdapter);
        if( pairedDevices.size() > 0 )
        {
        	for(BluetoothDevice device : pairedDevices ){
        		Log.d(device.getName(),device.getAddress());
        		_arrayAdapter.add(String.format("%s *",device.getName()));
        	}
        }
        
        /*
         * register receiver for blue tooth discovery 
         */
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(_receiver, filter);
        
        android.widget.Button button = (android.widget.Button)findViewById(R.id.button1);
        button.setOnClickListener( new View.OnClickListener(){
        	@Override
        	public void onClick(View v ){    	
        		startBluetoothDiscovery();
        	}
        });
        _blueList.setOnItemClickListener( new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView parent,View view,int position,long id) {
				Log.d("onitemclick",_arrayAdapter.getItem( position ));
			}
		});
        startBluetoothDiscovery();
    }
    /*
     * start bluetooth discovery
     */
    private void startBluetoothDiscovery()
    {
		_arrayAdapter.clear();
		_stopThread = new java.lang.Thread(){
				@Override
				public void run(){
					Thread thisThread = Thread.currentThread();
					int delay = 12*100;
					while( _stopThread==thisThread )
					{
						try{
							sleep(10);
						}catch(InterruptedException e)
						{
							Log.d("INFO","bluetooth cancelDiscovery for interrupt");
							_bluetoothAdapter.cancelDiscovery();
							return;
						}
						if( delay-- <= 0 )
						{
							Log.d("INFO","bluetooth cancelDiscovery");
							_bluetoothAdapter.cancelDiscovery();
							return;
						}
					}
				}
			};
		_stopThread.start();
		Log.d("INFO","bluetooth startDiscovery");
		_bluetoothAdapter.startDiscovery();    	
    }
    private final BroadcastReceiver _receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.d(device.getName(),device.getAddress());
                if( device.getBondState() == BluetoothDevice.BOND_NONE )
                	_arrayAdapter.add(String.format("%s",device.getName()));
                else
                	_arrayAdapter.add(String.format("%s *",device.getName()));
            }
        }
    };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    }
    /**
     * Callback method from {@link settingListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(settingDetailFragment.ARG_ITEM_ID, id);
            settingDetailFragment fragment = new settingDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.setting_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, settingDetailActivity.class);
            detailIntent.putExtra(settingDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
