package com.reprap.bluetooth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ScrollView;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;

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
	private ProgressDialog _progressDialog;
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
        _listView = (ListView)findViewById(R.id.listView1);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        _listView.setAdapter(_list);
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
    private static int CONNECT_ERROR_MSG = 1;
    private static int ADD_READ_MSG = 2;
    private Handler _handler = new Handler(){
    	@Override
    	public void handleMessage(final Message msg){
    		if( msg.what == CONNECT_ERROR_MSG ){
    			errorBox("Connect failed",(String)msg.obj);
    		}else if(msg.what==ADD_READ_MSG){
    			_list.add((String)msg.obj);
    		//	_listView.setSelection(0);
    		//	_listView.setSelectionAfterHeaderView();
    		}
    	}
    };
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
    					/*
    					 * 这里直接弹出对话栏会出错
    					 */
    					Message msg = new Message();
    					msg.what = CONNECT_ERROR_MSG;
    					msg.obj = e.toString();
    					_handler.sendMessage(msg);
    					return;
    				}
    				_progressDialog.cancel();
    				Thread thisThread = Thread.currentThread();
    				while(thisThread==_reciveThread){
    					try{
    						byte [] line = new byte[256];
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
        					Message msg = new Message();
        					msg.what = ADD_READ_MSG;
        					msg.obj = buf;
        					_handler.sendMessage(msg);    						
    					}catch(java.io.IOException e){
    						closeConnect();
    						Log.d(TAG,String.format("Thread exit,%s",e.toString()));
    						return;
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
    	super.onStart();
		_progressDialog = ProgressDialog.show(this,getText(R.string.connect_title),
				String.format((String)getText(R.string.connect_string),_device.getName()));
    	connectToDevice();
    }
    @Override
    public void onStop(){
    	super.onStop();
    	closeConnect();
    }
}
