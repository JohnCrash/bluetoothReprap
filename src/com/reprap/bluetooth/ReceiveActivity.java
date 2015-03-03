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
	private static final String TAG = "INFO";
    private static final int CONNECT_ERROR_MSG = 1;
    private static final int ADD_READ_MSG = 2;	 
    private static final int TRYAGIN_CMD_MSG = 3;
    private static final int TIMEOUT_MSG = 4;
    private static Handler _handler;
    
	public BluetoothDevice getBluetoothDevice(){
		return _device;
	}
	public synchronized boolean write(byte [] buffer ){
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
		Log.d(TAG,s);
		return writeString( s + "\r\n" );
	}
	/*
	 * reprap 指令发送与校验
	 * N1 G0 X10 *91
	 */
	protected static int _cmdLineNum = 1;	
	protected int _cmdState = 0;
	
	public int getLineNum(){
		return _cmdLineNum;
	}
	private static String _lastCmd = null;
	private static String _lastNCmd = null;
	public static final int WAITING_RESPENDS = -1;
	public static final int INVALID_VALUE = -2;
	public static final int OK_BUFFER = 2;
	public static final int OK_SEND = 1;

	public boolean isCmdBufferOver(){
		return _lastCmd != null;
	}
	/*
	 * 如果既没有要发的命令，同时打印机缓冲区命令都执行完成返回true
	 */
	public boolean isCmdBufferEmpty(){
		return _lastCmd == null;
	}
	/*
	 * 将命令发送给reprap的处理队列，如果队列满就返回false。
	 */
	public int cmdBufferImp(String cmd){
		if( cmd == null ){
			Log.d("ERROR","cmdBuffer cmd == null");
			return INVALID_VALUE;
		}
		//没有回应就不发送
		if(_lastCmd!=null)return WAITING_RESPENDS;

		int cs = 0;
		String sum_cmd;
		
		if( _cmdLineNum==1 )
			sum_cmd = String.format("N%d %s M110",_cmdLineNum++,cmd);
		else
			sum_cmd = String.format("N%d %s ",_cmdLineNum++,cmd);
		int len = sum_cmd.length();
		for( int i=0;i<len;++i ){
			cs ^= sum_cmd.charAt(i);
		}
		cs &= 0xff;		
		String ncmd = String.format("%s*%d",sum_cmd,cs);
		_lastCmd = cmd;
		_lastNCmd = ncmd;
		if( cmd.equals("M112") ){
			_lastCmd = null;
			_lastNCmd = null;
			_cmdLineNum = 1;
		}
		cmdRaw(ncmd);		
		return OK_SEND;
	}
	public int cmdBuffer(String cmd){
		return cmdBufferImp(cmd);
	}
	static ArrayDeque<String> _synCmds=new ArrayDeque<String>();
	public void synCmd(String cmd){
		if(_lastCmd!=null || !_synCmds.isEmpty())
		{
			_synCmds.add(cmd);
			return;
		}
		cmdBuffer(cmd);
	}
	public void cmdResult( String cmd,String info,int result ){
		
	}
	public void completeCmd(){
		_lastCmd = null;
		_lastNCmd = null;
		if( !_synCmds.isEmpty() )
		{
			cmdBuffer(_synCmds.poll());
		}
	}

	marlin _checker = new marlin();
	public void receiver( byte [] buf ){
		String s = new String(buf,0,buf.length);
		if( _lastCmd == null ){
			Log.d(TAG,"receviver data on _lastCmd == null");
			return;
		}
		String cmd = _lastCmd;
		int r = _checker.result(cmd, s);
		if( r==gcode.OK )
			completeCmd();
		else if( r==gcode.RESEND ){
			Log.d(TAG,"Resend:"+_lastNCmd);
			cmdRaw(_lastNCmd);
		}
			
		cmdResult( cmd,s,r );
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
    							if( b == '\r' )continue;
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
