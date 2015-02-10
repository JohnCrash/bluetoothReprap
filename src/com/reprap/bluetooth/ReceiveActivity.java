package com.reprap.bluetooth;

import android.support.v4.app.FragmentActivity;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;

public class ReceiveActivity extends Activity  {
	private static BluetoothDevice _device;
	private static Thread _reciveThread;
	private static Thread _timeoutThread;
	private String TAG = "INFO";
    private static final int CONNECT_ERROR_MSG = 1;
    private static final int ADD_READ_MSG = 2;	 
    private static final int TRYAGIN_CMD_MSG = 3;
    private static final int TIMEOUT_MSG = 4;
    private static Handler _handler;
    
	public BluetoothDevice getBluetoothDevice(){
		return _device;
	}
	public boolean write(byte [] buffer ){
		OutputStream out = settingListActivity.getOutputStream();
		if( out == null )return false;
		try{
			out.write( buffer );
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
	public boolean cmdRaw( String s ){
		return writeString( s + "\r\n" );
	}
	/*
	 * reprap 指令发送与校验
	 * N1 G0 X10 *91
	 */
	protected int _cmdLineNum = 1;
	protected static final Pattern errorMsg = Pattern.compile("Error:([\\s\\S]*)");
	protected static final Pattern resend = Pattern.compile("Resend:(\\d+)");
	protected static final Pattern okPattern = Pattern.compile("ok([\\s\\S]*)");
	
	protected int _cmdState = 0;
	
	public int getLineNum(){
		return _cmdLineNum;
	}
	private int MAX_BUFFER = 4;
	private ArrayDeque<String> _cmdWaitResponsQueue = new ArrayDeque<String>();
	/*
	 * 将命令发送给reprap的处理队列，如果队列满就返回false。
	 */
	public boolean cmdBuffer(String cmd){
		synchronized(_cmdWaitResponsQueue){
			if( _cmdWaitResponsQueue.size() >= MAX_BUFFER )
				return false;
			_cmdWaitResponsQueue.add(cmd);
		}
		int cs = 0;
		String sum_cmd = String.format("N%d %s ",_cmdLineNum++,cmd);
		int len = sum_cmd.length();
		for( int i=0;i<len;++i ){
			cs ^= sum_cmd.charAt(i);
		}
		cs &= 0xff;		
		String ncmd = String.format("%s*%d",sum_cmd,cs);
		cmdRaw(ncmd);
		return true;
	}
	/*
	 * 当缓冲最大数设置为1时，缓冲算法蜕化为顺序命令方式
	 */
	public void setCmdBufferMaxCount(int c){
		MAX_BUFFER = c;
	}
	public void cmdResult( String cmd,String result ){
		
	}
	public void receiver( byte [] buf ){
		String s = new String(buf,0,buf.length);
		Matcher mok = okPattern.matcher(s);
		String cmd;
		if( mok.find() ){
			synchronized(_cmdWaitResponsQueue){
				cmd = _cmdWaitResponsQueue.poll();
			}
			cmdResult( cmd,s );
		}else{
			Matcher m = errorMsg.matcher(s);
			if( m.find() ){
				//Error:checksum ...
				Log.d("ERROR",s);
				synchronized(_cmdWaitResponsQueue){
					cmd = _cmdWaitResponsQueue.peek();
				}
				cmdResult( cmd,s );
			}else{
				synchronized(_cmdWaitResponsQueue){
					cmd = _cmdWaitResponsQueue.peek();
				}
				cmdResult( cmd,s );
			}
		}
	}
	 protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_cmdLineNum = 1;
		BluetoothDevice device = settingListActivity.getBluetoothDevice();
		if( device ==null){
			Log.d(TAG,"Console activity can't launch,devic == null");
		}
		if( device.getBondState() != BluetoothDevice.BOND_BONDED){
			Log.d(TAG,"Console activity can't launch,devic not paired");
			setTitle(String.format("Console (%s) - not paired",device.getName()));
			errorBox(getText(R.string.error).toString(),getText(R.string.lanuch_error).toString());
		    return;
		 }
		_handler = new Handler(){
		    	@Override
		    	public void handleMessage(final Message msg){
		    		switch( msg.what ){
		    		case CONNECT_ERROR_MSG:
		    			errorBox(getText(R.string.error).toString(),(String)msg.obj);
		    			break;
		    		case ADD_READ_MSG:
		    			receiver((byte [])msg.obj);
		    			break;
		    		case TRYAGIN_CMD_MSG:
		    			break;
		    		case TIMEOUT_MSG:
		    			break;
		    		}
		    	}
		    };		
		if(_reciveThread==null){
			_device = device;
		}
	 }
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
    	if( _handler == null )return;
		Message msg = new Message();
		msg.what = id;
		msg.obj = obj;
		_handler.sendMessage(msg);
    }

   private void reciverThread(){
    	try{
    		if( _reciveThread != null )
    			return;
    		_reciveThread = new Thread(){
    			@Override
    			public void run(){
    				Thread thisThread = Thread.currentThread();
    				final byte [] line = new byte[256];
    				while(thisThread==_reciveThread){
    					try{
    						int i = 0;
    						InputStream in = settingListActivity.getInputStream();
    						if( in == null ){
    							_reciveThread = null;
    							return;
    						}
    						do{
    							int b = in.read();
    							if( _reciveThread == null ){
    								Log.d(TAG,"reciverThread exit");
    								return; 
    							}
    							if( b == -1 )continue;
    							if( b == '\t' )continue;
    							if( b == '\n' )
    								break;
    							line[i++] = (byte)b;
    						}while( i < 256  );
    						if( i > 0 ){
    							byte [] buffer = new byte[i];
    							for( int j=0;j<i;j++)buffer[j] = line[j];
    							sendMessage(ADD_READ_MSG,buffer);
    						}
    					}catch(Exception e){
    						_reciveThread = null;
    						Log.d(TAG,String.format("Thread exit,%s",e.toString()));
    						//sendMessage( CONNECT_ERROR_MSG,e.toString());
    						return;
    					}
    				}
    			}
    		};
    		_reciveThread.start();
    	}catch(Exception e){
    		_reciveThread = null;
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
    	super.onStop();
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    }
}
