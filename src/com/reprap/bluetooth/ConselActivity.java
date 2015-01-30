package com.reprap.bluetooth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.AdapterView;

import java.io.InputStream;
import java.io.OutputStream;

import android.view.View;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

public class ConselActivity extends Activity {
	private EditText _input;
	private ArrayAdapter<String> _list;
	private ListView _listView;
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
        _in = settingListActivity.getInputStream();
        _out = settingListActivity.getOutputStream();
        _socket = settingListActivity.getBluetoothSocket();
        
        setTitle(String.format("Console (%s)",device.getName()));
        _input = (EditText)findViewById(R.id.editText1);
        _listView = (ListView)findViewById(R.id.listView1);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        _listView.setAdapter(_list);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
        	@Override
        	public void onItemClick(AdapterView<?> parent,View view,int position,long id){
        		if( _out != null ){
        			String cmd = _list.getItem(position);
        			cmd += "\r\n";
        			_list.add(cmd);
        			try{
        				_out.write(cmd.getBytes());
        			}catch(Exception e){         					
        				new AlertDialog.Builder(ConselActivity.this).setTitle("Send failed")
    					.setMessage(e.toString()).show();}
        		}
        }});
        _input.setOnEditorActionListener(new OnEditorActionListener(){
        	@Override
        	 public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
        		String cmd = v.getText().toString();
        		if(cmd.length()>0){ 
        			v.setText("");
        			if( _out !=	null ){
        				try{
        					cmd += "\r\n";
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
    			_list.add((String)msg.obj);
    			break;
    		}
    	}
    };
    private void sendMessage( int id,Object obj){
		Message msg = new Message();
		msg.what = id;
		msg.obj = obj;
		_handler.sendMessage(msg);
    }
    private void reciverThread(){
    	try{
    		final byte [] line = new byte[256];
    		_reciveThread = new Thread(){
    			@Override
    			public void run(){
    				Thread thisThread = Thread.currentThread();
    				while(thisThread==_reciveThread){
    					try{
    						int i = 0;
    						do{
    							int b = _in.read();
    							if( b == -1 )continue;
    							if( b == '\t' )continue;
    							if( b == '\n' )
    								break;
    							line[i++] = (byte)b;
    						}while( i < 256  );
    						String buf = new String(line,0,i);
    						Log.d(TAG,buf);
    						/*
    						 * 这里直接操作_list.add会出错
    						 */
    						sendMessage(ADD_READ_MSG,buf);
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
