package com.reprap.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import java.util.HashMap;

import android.widget.AdapterView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.widget.EditText;
import android.view.inputmethod.InputMethodManager;
import android.bluetooth.BluetoothSocket;

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
    private HashMap<String,BluetoothDevice> _DeviceByName;
    private String _selectDeviceName;
    private EditText _input;
    private static BluetoothDevice _device;
	private static BluetoothSocket _socket;
	private static InputStream _in;
	private static OutputStream _out;    
	private static Thread _reciveThread;
    private static String TAG = "INFO";
    private static final int CONNECT_ERROR_MSG = 1;
    private static final int CONNECT_SUCCESS_MSG = 2;
	private ProgressDialog _progressDialog;
	
    public static InputStream getInputStream(){
    	return _in;
    }
    public static OutputStream getOutputStream(){
    	return _out;
    }
    public static BluetoothSocket getBluetoothSocket(){
    	return _socket;
    }
    public static BluetoothDevice getBluetoothDevice(){
    	return _device;
    }
    private void errorBox(String title,String info){
		new AlertDialog.Builder(settingListActivity.this).setTitle(title)
		.setMessage(info)
		.setNegativeButton("Close", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				settingListActivity.this.finish();
			}
		})
		.show(); 
    }
    private Handler _handler = new Handler(){
    	@Override
    	public void handleMessage(final Message msg){
    		switch(msg.what){
    		case CONNECT_ERROR_MSG :
    			errorBox("Connect failed",(String)msg.obj);
    			break;
    		case CONNECT_SUCCESS_MSG:
				Intent intent = new Intent(settingListActivity.this,CommandActitivy.class);
				if( intent != null ){
					intent.putExtra("device",_device);
					startActivity(intent);
					return;
				}    			
    			break;
    		}
    	}
    };    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG,"onCreate is called~");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blue_enum_interface);
        setTitle(R.string.select_bluetooth);
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
        	new AlertDialog.Builder(this).setTitle("ERROR")
        	.setMessage("Device does not support Bluetooth.")
        	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//exit
					System.exit(0);
				}
			})
        	.show();
        	return;
        }
        if( !_bluetoothAdapter.isEnabled() )
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);        	
        }
        _DeviceByName = new HashMap<String,BluetoothDevice>();
        Set<BluetoothDevice> pairedDevices =_bluetoothAdapter.getBondedDevices();
        _blueList = (ListView)findViewById(R.id.listView1);

        _arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        _blueList.setAdapter(_arrayAdapter);
        if( pairedDevices.size() > 0 )
        {
        	for(BluetoothDevice device : pairedDevices ){
        		Log.d(device.getName(),device.getAddress());
        		_arrayAdapter.add(device.getName());
        		_DeviceByName.put( device.getName(),device);
        	}
        }
        
        /*
         * register receiver for blue tooth discovery 
         */
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(_receiver, filter);
        
        android.widget.Button button = (android.widget.Button)findViewById(R.id.button1);
        button.setText(R.string.scan_bluetooth);
        button.setOnClickListener( new View.OnClickListener(){
        	@Override
        	public void onClick(View v ){    	
        		startBluetoothDiscovery();
        	}
        });
        _blueList.setOnItemClickListener( new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent,View view,int position,long id) {
				BluetoothDevice device = _DeviceByName.get(_arrayAdapter.getItem(position));				
				if( device != null &&
						(device.getBondState() == BluetoothDevice.BOND_BONDED || device.getBondState()==BluetoothDevice.BOND_BONDING) ){
					//ConselActivity
					_bluetoothAdapter.cancelDiscovery();
					if( _socket !=null ){
						try{
							if( _in == null )
								_in = _socket.getInputStream();
							if( _out == null )
								_out = _socket.getOutputStream();
						}catch(Exception e){
							Log.d(TAG,e.toString());
							sendMessage(CONNECT_ERROR_MSG,e.toString());
						}
						sendMessage(CONNECT_SUCCESS_MSG,null);
					}else{
						closeConnect();
						_device = device;
						connectToDevice( device );
					}
				}else if(device != null) {
					/*not piared yet*/
					pairDevice(device);
				}else{
					Log.d(TAG,"bluetooth device = null");
				}
			}
		});
        startBluetoothDiscovery();
    }
    private void sendMessage( int id,Object obj){
		Message msg = new Message();
		msg.what = id;
		msg.obj = obj;
		_handler.sendMessage(msg);
    }
    private void connectToDevice(final BluetoothDevice device){
    	try{
    		_progressDialog = ProgressDialog.show(this,getText(R.string.connect_title),
    				String.format((String)getText(R.string.connect_string),_device.getName()));    		
    		_reciveThread = new Thread(){
    			@Override
    			public void run(){
    				try{
	    	    		java.lang.reflect.Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
	    	    		_socket = (BluetoothSocket)m.invoke(device,1);
	    	    		_socket.connect();
	    	    		_in = _socket.getInputStream();
	    	    		_out = _socket.getOutputStream();
	    	    		_progressDialog.cancel();
    				}catch(Exception e){
    					closeConnect();
    					/*
    					 * 这里直接弹出对话栏会出错
    					 */
    					sendMessage(CONNECT_ERROR_MSG,e.toString());
    					return;
    				}
    				sendMessage(CONNECT_SUCCESS_MSG,null);
    			}
    		};
    		_reciveThread.start();
    	}catch(Exception e){
    		closeConnect();
    		Log.d(TAG,e.toString());
    		sendMessage(CONNECT_ERROR_MSG,e.toString());
    	}
    }
    static public void closeStream(){
    	if( _in != null )
    	{
    		try{
    			_in.close();
    		}catch(Exception e){}
    		_in = null;
    	}
    	if( _out != null )
    	{
    		try{
    			_out.close();
    		}catch(Exception e){}
    		_out = null;
    	}    	
    }
    static public void closeConnect(){
    	_reciveThread = null;
    	closeStream();
    	if( _socket != null )
    	{
    		try{
    		_socket.close();
    		}catch(Exception e ){}
    		_socket = null;
    	}
    }
    
    private void pairByPin( BluetoothDevice device){
		_selectDeviceName = device.getName();
        AlertDialog dialog = new AlertDialog.Builder(settingListActivity.this)  
        .setIcon(android.R.drawable.btn_star_big_on) 
        .setTitle("Input PIN")
        .setPositiveButton("Cancel", _onClickPair)  
        .setNegativeButton("OK",  _onClickPair).create();
        _input = new EditText(settingListActivity.this);
        dialog.setView(_input);
        dialog.show();
        /*
         *  show softboard
         */
        _input.setFocusable(true);
        _input.requestFocus();
        (new  android.os.Handler()).postDelayed(new java.lang.Runnable() {
        	@Override
        		public void run() {
    			InputMethodManager imm = (InputMethodManager)_input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    			imm.showSoftInput(_input, InputMethodManager.SHOW_FORCED);
        	}},100);
    }
    private void pairDevice( BluetoothDevice device){
        try {
            Log.d("pairDevice()", "Start Pairing...");
            java.lang.reflect.Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d(TAG, "Pairing finished.");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
    private void unpairDevice(BluetoothDevice device) {
        try {
            Log.d(TAG, "Start Un-Pairing...");
            java.lang.reflect.Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d(TAG, "Un-Pairing finished.");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
    private final AlertDialog.OnClickListener _onClickPair = new AlertDialog.OnClickListener(){
    	@Override
    	public void onClick(DialogInterface dialog,int whitch)
    	{
    		if( _input != null  ){
    			InputMethodManager imm = (InputMethodManager)_input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    			imm.hideSoftInputFromWindow(_input.getWindowToken(), 0);
    		}
    			
    		if( whitch == Dialog.BUTTON_NEGATIVE ){
    			BluetoothDevice device = _DeviceByName.get(_selectDeviceName);
    			if( device != null && _input != null ){
    				Log.d("PIN",String.format("%s",_input.getText()));
    				 CharSequence text = _input.getText();
    				 if( text.length() > 0 ){
    					 byte [] pin = new byte[text.length()];
    					 for( int i = 0;i<text.length();i++)
    						 pin[i] = (byte)text.charAt(i);
    					// Log.d(settingListActivity.TAG,String.format("setPin result:%s",device.setPin(pin)?"true":"error"));
    				 }
    			}else{
    				Log.d(TAG,"device = null");
    			}
    		}
    	}
    };
    /*
     * start bluetooth discovery
     */
    private void startBluetoothDiscovery()
    {
		_arrayAdapter.clear();
		_DeviceByName.clear();
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
							Log.d(TAG,"bluetooth cancelDiscovery for interrupt");
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
		Log.d(TAG,"bluetooth startDiscovery");
		_bluetoothAdapter.startDiscovery();    	
    }
    private BroadcastReceiver _receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.d(device.getName(),device.getAddress());
                _arrayAdapter.add(String.format("%s",device.getName()));
                _DeviceByName.put( device.getName(),device);
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
    @Override
    public void onPause(){
    	super.onPause();
    	Log.d(TAG,"onPause is called");
    }
    @Override
    public void onStop(){
    	super.onStop();
    	Log.d(TAG,"onStop is called");
    }
    @Override
    public void onDestroy(){
    	closeConnect();
    	Log.d(TAG,"onDestroy is called");
    	super.onDestroy();
    }
    @Override
    public void onResume(){
    	super.onResume();
    	Log.d(TAG,"onResume is called");
    }
    @Override
    public void onStart(){
    	super.onStart();
    	Log.d(TAG,"onStart is called");
    }
}
