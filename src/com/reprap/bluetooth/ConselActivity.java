package com.reprap.bluetooth;

import android.os.Bundle;
import android.util.Log;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.app.Activity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Dialog;

public class ConselActivity extends Activity {
	private EditText _input;
	private ArrayAdapter<String> _list;
	private BluetoothDevice _device;
	private BluetoothSocket _socket;
	private InputStream _in;
	private OutputStream _out;
	private Thread _reciveThread;
	private String TAG = "INFO";
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consol_layout);
        Intent intent = getIntent();
        BluetoothDevice device = (BluetoothDevice)intent.getExtras().getParcelable("device");
        if( device ==null){
        	Log.d(TAG,"Console activity can't launch,devic == null");
        }
        if( device.getBondState() != BluetoothDevice.BOND_BONDED){
        	Log.d(TAG,"Console activity can't launch,devic not paired");
        	setTitle(String.format("Console (%s) - not paired",device.getName()));
        	return;
        }
        _device = device;
        setTitle(String.format("Console (%s)",device.getName()));
        _input = (EditText)findViewById(R.id.editText1);
        ListView lv = (ListView)findViewById(R.id.listView1);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        lv.setAdapter(_list);
        _input.setOnEditorActionListener(new OnEditorActionListener(){
        	@Override
        	 public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
        		String cmd = v.getText().toString();
        		if(cmd.length()>0){ 
        			v.setText("");
        			if( _out !=	null ){
        				try{
        					_out.write(cmd.getBytes());
        				}catch(Exception e){
        					new AlertDialog.Builder(ConselActivity.this).setTitle("Send failed")
        					.setMessage(e.toString()).show();
        				}
        			}
        			_list.add(cmd);
        		}
        		return true;
        	}
        });
    }
    private void errorBox(String title,String info){
		new AlertDialog.Builder(ConselActivity.this).setTitle(title)
		.setMessage(info)
		.setNegativeButton("Close", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				ConselActivity.this.finish();
			}
		})
		.show(); 
    }
    private void connectToDevice(){
    	try{
    		_reciveThread = new Thread(){
    			@Override
    			public void run(){
    				try{
	    	    		java.lang.reflect.Method m = _device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
	    	    		_socket = (BluetoothSocket)m.invoke(_device,1);
	    	    		_socket.connect();
	    	    		_in = _socket.getInputStream();
	    	    		_out = _socket.getOutputStream();
    				}catch(Exception e){
    					closeConnect();
    					errorBox("Connect failed",e.toString());
    					return;
    				}
    				Thread thisThread = Thread.currentThread();
    				while(thisThread==_reciveThread){
    					try{
    						byte [] line = new byte[256];
    						int i = 0;
    						do{
    							int b = _in.read();
    							if( b == -1 )continue;
    							if( b == '\n' )
    								break;
    							line[i++] = (byte)b;
    						}while( i < 256  );
    						String buf = new String(line,0,i);
    						Log.d(TAG,buf);
    						//_list.add(buf);
    					}catch(java.io.IOException e){
    						closeConnect();
    						errorBox("Receive failed",e.toString());
    					}
    				}
    			}
    		};
    		_reciveThread.start();
    	}catch(Exception e){
    		closeConnect();
    		Log.d(TAG,e.toString());
    		//messagebox
    		new AlertDialog.Builder(this).setTitle("Connect failed")
    			.setMessage(e.toString()).show();
    	}
    }
    private void closeConnect(){
    	_reciveThread = null;
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
    	if( _socket != null )
    	{
    		try{
    		_socket.close();
    		}catch(Exception e ){}
    		_socket = null;
    	}
    }

    @Override
    public void onStart(){
    	connectToDevice();
    }
    @Override
    public void onStop(){
    	closeConnect();
    }
}
