package com.reprap.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;

import java.io.InputStream;
import java.io.OutputStream;

public class ReceiveActivity extends Activity  {
	private BluetoothDevice _device;
	private BluetoothSocket _socket;
	private InputStream _in;
	private OutputStream _out;
	private Thread _reciveThread;
	private String TAG = "INFO";
	
	public BluetoothDevice getBluetoothDevice(){
		return _device;
	}
	public BluetoothSocket getBluetoothSocket(){
		return _socket;
	}
	public boolean write(byte [] buffer ){
		if( _out == null )return false;
		try{
		_out.write( buffer );
		}catch(Exception e){
			Log.d(TAG,e.toString());
			sendMessage( CONNECT_ERROR_MSG,e.toString());
			return false;
		}
		return true;
	}
	public boolean writeString( String s ){
		return write(s.getBytes());
	}
	public void receiver( byte [] buf ){
	}
	 protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		BluetoothDevice device = (BluetoothDevice)intent.getExtras().getParcelable("device");
		if( device ==null){
			Log.d(TAG,"Console activity can't launch,devic == null");
		}
		if( device.getBondState() != BluetoothDevice.BOND_BONDED){
			Log.d(TAG,"Console activity can't launch,devic not paired");
			setTitle(String.format("Console (%s) - not paired",device.getName()));
			errorBox(getText(R.string.error).toString(),getText(R.string.lanuch_error).toString());
		    return;
		 }		 
		 _device = device;
		 _in = settingListActivity.getInputStream();
		 _out = settingListActivity.getOutputStream();
		 _socket = settingListActivity.getBluetoothSocket();
	 }
	    private static final int CONNECT_ERROR_MSG = 1;
	    private static final  int ADD_READ_MSG = 2;	 
	    private Handler _handler = new Handler(){
	    	@Override
	    	public void handleMessage(final Message msg){
	    		switch( msg.what ){
	    		case CONNECT_ERROR_MSG:
	    			errorBox(getText(R.string.error).toString(),(String)msg.obj);
	    			break;
	    		case ADD_READ_MSG:
	    			receiver((byte [])msg.obj);
	    			break;
	    		}
	    	}
	    };
	    private void errorBox(String title,String info){
			new AlertDialog.Builder(ReceiveActivity.this).setTitle(title)
			.setMessage(info)
			.setNegativeButton("Close", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					ReceiveActivity.this.finish();
				}
			})
			.show(); 
	    }
	    private void sendMessage( int id,Object obj){
			Message msg = new Message();
			msg.what = id;
			msg.obj = obj;
			_handler.sendMessage(msg);
	    }
	   private void reciverThread(){
	    	try{
	    		_reciveThread = new Thread(){
	    			@Override
	    			public void run(){
	    				Thread thisThread = Thread.currentThread();
	    				final byte [] line = new byte[256];
	    				while(thisThread==_reciveThread){
	    					try{
	    						int i = 0;
	    						do{
	    							int b = _in.read();
	    							if( _reciveThread == null ){
	    								Log.d(TAG,"reciverThread exit");
	    								break; 
	    							}
	    							if( b == -1 )continue;
	    							if( b == '\t' )continue;
	    							if( b == '\n' )
	    								break;
	    							line[i++] = (byte)b;
	    						}while( i < 256  );
	    						//String buf = new String(line,0,i);
	    						//Log.d(TAG,buf);
	    						/*
	    						 * 这里直接操作_list.add会出错
	    						 */
	    						if( i > 0 ){
	    							byte [] buffer = new byte[i];
	    							for( int j=0;j<i;j++)buffer[j] = line[j];
	    							sendMessage(ADD_READ_MSG,buffer);
	    						}
	    					}catch(java.io.IOException e){
	    						Log.d(TAG,String.format("Thread exit,%s",e.toString()));
	    						sendMessage( CONNECT_ERROR_MSG,e.toString());
	    						return;
	    					}
	    				}
	    			}
	    		};
	    		_reciveThread.start();
	    	}catch(Exception e){
	    		Log.d(TAG,e.toString());
	    		sendMessage( CONNECT_ERROR_MSG,e.toString());
	    	}
	    }	 
	    @Override
	    public void onStart(){
	    	super.onStart();
	    	reciverThread();
	    }	   
	    @Override
	    public void onStop(){
	    	_reciveThread = null;
	    	super.onStop();
	    }	   
}
