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
	protected String _lastCmd;
	protected String _lastCmdOrigin;
	protected long _lastCmdTime = 0;
	protected int _tryCount = 0;
	private long TIME_OUT = 2000;
	private static final int MAX_TRY = 3;
	private static final Pattern errorMsg = Pattern.compile("Error:([\\s\\S]*)");
	private static final Pattern resend = Pattern.compile("Resend:(\\d+)");
	public static final int CMD_OK = 0;
	public static final int CMD_TIMEOUT = 1;
	public static final int CMD_ERROR = 2;
	public static final int CMD_WAIT = 3;
	public static final int STATE_TAG = 1;
	public static final int INFO_TAG = 2;
	
	protected int _cmdState = 0;
	
	public long getTimeOut(){
		return TIME_OUT;
	}
	public void setTimeOut(long t){
		TIME_OUT = t;
	}
	public int getLineNum(){
		return _cmdLineNum;
	}
	public String getLastCmd(){
		return _lastCmd;
	}
	public String getLastCmdOrigin(){
		return _lastCmdOrigin;
	}
	public boolean cmdSum( String s ){
		int cs = 0;
		long time = System.currentTimeMillis();
		if( _lastCmd==null ){
			String cmd = String.format("N%d %s ",_cmdLineNum,s);
			int len = cmd.length();
			for( int i=0;i<len;++i ){
				cs ^= cmd.charAt(i);
			}
			cs &= 0xff;
			_lastCmd = String.format("%s*%d",cmd,cs);
			_cmdLineNum++;
			_lastCmdTime = time;
			_cmdState = CMD_WAIT;
			_lastCmdOrigin = s;
			return cmdRaw(_lastCmd);
		}
		return false;
	}
	public int getCmdState(){
		return _cmdState;
	}
	public void cmdResult(int tag,int result,String info ){
		
	}
	public boolean receiver( byte [] buf ){
		String s = new String(buf,0,buf.length);
		//Error:checksum ...
		if( s == "ok" ){
			//成功
			_cmdState = CMD_OK;
			cmdResult(STATE_TAG,_cmdState,s);
			_lastCmd = null;
			return true;
		}else if(_lastCmd!=null){
			//校验失败
			Matcher m = errorMsg.matcher(s);
			if( m.find() ){
				Log.d(TAG,s);
				Log.d(TAG,_lastCmd);
				cmdResult(INFO_TAG,_cmdState,s);
				return true;
			}
			//处理重发请求
			m = resend.matcher(s);
			if( m.find() ){
				int n = Integer.getInteger(m.group(1)).intValue();
				if( n==_cmdLineNum-1 && _tryCount < MAX_TRY ){
					_tryCount++;
					return cmdRaw(_lastCmd); 
				}else{
					Log.d(TAG,getString(R.string.resend_error));
					Log.d(TAG,s);
					Log.d(TAG,_lastCmd);
					_cmdState = CMD_ERROR;
					cmdResult(STATE_TAG,_cmdState,s);
					_tryCount = 0;
					_lastCmd = null;
				}
			}
		}else{
			cmdResult(INFO_TAG,_cmdState,s);
		}
		return false;
	}
	 protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
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
		    			cmdResult(STATE_TAG,_cmdState,"time out");
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
	   private void timeoutThread(){
   			if( _timeoutThread != null )return;
   			_timeoutThread = new Thread(){
   				@Override
   				public void run(){
   					Thread thisThread = Thread.currentThread();
   					while(thisThread==_timeoutThread){
   						try{
   							if( _lastCmd!=null ){
   								long time = System.currentTimeMillis();
   								if(time-_lastCmdTime>TIME_OUT){
   									Log.d(TAG,getString(R.string.try_count_over));
   									Log.d(TAG,_lastCmd);
   									_tryCount = 0;
   									_lastCmd = null;
   									_cmdState = CMD_TIMEOUT;
   									sendMessage(TIMEOUT_MSG,null);
   								}   								
   							}
   							sleep(10);
   						}catch(Exception e){
   							_timeoutThread = null;
   							return;
   						}
   					}
   					_timeoutThread = null;
   				}
   			};
   			_timeoutThread.start();
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
	    	timeoutThread();
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
