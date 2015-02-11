package com.reprap.bluetooth;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.ListView;
import android.util.Pair;

public class PrintingActivity extends ReceiveActivity{
	private static String _printFileName = null;
	private static String _sdFileName = null;
	private static File _printFile = null;
	private static BufferedInputStream _in = null;
	private static boolean _printPause;
	static final int cont = R.id.button1;
	static final int pause = R.id.button2;
	static final int estop = R.id.button3;
	static final int stop = R.id.button4;
	static final int [] button_id = {cont,pause,estop,stop};
	
	protected void initAllEvent(){
		for( int i = 0;i < button_id.length;i++ ){
			final int id = button_id[i];
			findViewById(id).setOnClickListener(new View.OnClickListener(){
	        	@Override
	        	public void onClick(View view){
	        		PrintingActivity.this.onClick(id,(Button)view);
	        	}
	        });
		}
	}	
	private static int mode = 0;
	private static long fileLength;
	private static long printOffset;
	private TextView _progressText;
	private TextView _resultText;
	private ProgressBar _progress;
	private ListView _listview;
	private ArrayAdapter<String> _list;
	private static final int PROGRESS_MSG = 1;
	private static final int MSGBOX_MSG = 2;
	private static final int OK_MSG = 3;
	private static final int ERROR_DIALOG = 1;
	private static final int OK_DIALOG = 2;
	private Handler _handler = new Handler(){
    	@Override
    	public void handleMessage(final Message msg){
    		switch( msg.what ){
    		case PROGRESS_MSG:
    			{
    				Pair<String,String> p = (Pair<String,String>)msg.obj;
    				float d = Float.parseFloat(p.first);
    				_progress.setProgress((int)(d*100));
    				_progressText.setText(String.format(getString(R.string.progress_format),(int)(d*100)));
    				_resultText.setText(p.second);
    				break;
    			}
    		case MSGBOX_MSG:
    			{
    				Pair<String,String> p = (Pair<String,String>)msg.obj;
    				msgBox(ERROR_DIALOG,p.first,p.second);
    			}
    			break;
    		case OK_MSG:
	    		{
					Pair<String,String> p = (Pair<String,String>)msg.obj;
					msgBox(OK_DIALOG,p.first,p.second);
					break;
	    		}
    		}
    	}
    };
    private void msgBox(int type,String title,String info){
    	AlertDialog.Builder builder = new AlertDialog.Builder(PrintingActivity.this);
    	builder.setTitle(title);
    	builder.setMessage(info);
    	if( type == ERROR_DIALOG ){
	    	builder.setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					PrintingActivity.this.finish();
				}
			});
    	}else if( type == OK_DIALOG ){
	    	builder.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
				}
			});    		
    	}
		builder.show(); 
    }
    private void sendMessage(int code,String title,String info){
    	if( _handler == null )return;
		Message msg = new Message();
		msg.what = code;
		msg.obj = new Pair<String,String>(title,info);
		_handler.sendMessage(msg);
    }
	private void onClick( int id,Button button ){
		switch(id){
		case cont:
			if( mode == 1 ){
				_printPause = false;
			}else if( mode == 2 ){
				cmdBuffer("M24");
			}
			break;
		case pause:
			if( mode == 1 ){
				_printPause = true;
			}else if( mode == 2 ){
				cmdBuffer("M25");
			}
			break;
		case estop:
			mode = 0;
			try{_in.close();_in=null;}catch(Exception e){}
			_printPause = false;
			_printThread = null;
			cmdBuffer("M112");
			break;
		case stop:
			if( mode == 1 ){
				try{_in.close();_in=null;}catch(Exception e){}
				_printPause = false;
				_printThread = null;
			}else if( mode == 2 ){
				cmdBuffer("M24");
			}
			break;
		}
	}
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_printing));
        setContentView(R.layout.printing_interface);
        initAllEvent();
        _progressText = (TextView)findViewById(R.id.textView1);
        _resultText = (TextView)findViewById(R.id.textView2);
        _listview = (ListView)findViewById(R.id.listView1);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        _listview.setAdapter(_list);
        
        _progress = (ProgressBar)findViewById(R.id.progressBar1);
        _printFileName = getIntent().getStringExtra("file");
        if( _printFileName != null ){
        	//file print
        	if( mode == 0 ){
        		mode = 1;
	        	try{
	        		_printFile = new File(_printFileName);
	        		fileLength = _printFile.length();
	        		if( fileLength > 0 ){
	        			printOffset = 0;
	        			_in = new BufferedInputStream(new FileInputStream(_printFile) );
	        			bluetoothPrintThread();
	        		}else{
	        			Log.d("ERROR","GCODE file length must > 0");
	        		}
	        	}catch(Exception e){
	        		Log.d("ERROR",e.getMessage());
	        	}
        	}
        }else{
        	//sd print
        	_sdFileName = getIntent().getStringExtra("sd");
        	if( _sdFileName != null ){
        		mode = 2;
        		cmdBuffer("M21");
        		cmdBuffer(String.format("M23 %s", _sdFileName.toLowerCase()));
        		cmdBuffer("M24");
        	}
        	sdPrintThread();
        }
    }
    static Thread _printThread;
    protected void bluetoothPrintThread(){
    	_printPause = false;
    	_printThread = new Thread(){
    		@Override
    		public void run(){
    			byte [] line = new byte[256];
    			Thread thisThread = Thread.currentThread();
    			while(thisThread==_printThread){
    				int i = 0;
    				if( _in == null ){return;}
    				try{
    					do{
    						int b = _in.read();
    						if( _printThread != thisThread ){
    							return;
    						}
    						if( b == -1 ){
    							//End of Stream
    							thisThread = null;
    							_in.close();
    							_in = null;
    							break;
    						}
    						if( b == '\r')continue;
    						if( b == '\n')
    							break;
    						line[i++] = (byte)b;
    					}while( i < 256 );
    					printOffset += (i+2); //include /r/n
    					while( _printPause ){
    						sleep(100);
    					}
    					if( i > 0 ){
    						String cmd = new String(line,0,i); 
    						cmdBuffer(cmd);
    						while( isCmdBufferOver() ){
    							sleep(50);
    							if( _printThread != thisThread ){
    								_printThread = null;
    								return;
    							}
    						}
    						float v = (float)printOffset/(float)fileLength;
    						String progress = Float.toString(v);
    						sendMessage(PROGRESS_MSG,progress,cmd);
    					}
    				}catch(Exception e){
    					_printThread = null;
    					Log.d("ERROR",e.getMessage());
    					return;
    				}
    			}
    			sendMessage(PROGRESS_MSG,"1","Print complete");
    			sendMessage(OK_MSG,getString(R.string.COMPLETE),getString(R.string.COMPLETE_INFO));
    		}
    	};
    	_printThread.start();
    }
    static Thread _sdstatusThread = null;
    protected void sdPrintThread(){
    	_sdstatusThread = new Thread(){
    		@Override
    		public void run(){
    			Thread thisThread = Thread.currentThread();
    			try{
	    			while(thisThread == _sdstatusThread){
	    				cmdBuffer("M27");
	    				sleep(1000*5);
	    			}
    			}catch(Exception e){    				
    			}
    		}
    	};
    	_sdstatusThread.start();
    }
    Pattern _printStatusPattern = Pattern.compile("^SD printing byte (\\d+)/(\\d+)");
    Pattern _printStatusFailed = Pattern.compile("^Not SD printing");
    @Override
    public void cmdResult( String cmd,String info ){
    	Matcher m = _printStatusPattern.matcher(info);
    	if( m.find() ){
    		completeCmd();
    		String s1 = m.group(1);
    		String s2 = m.group(2);
    		printOffset = Integer.parseInt(s1);
    		fileLength = Integer.parseInt(s2);
    		if( fileLength > 0 )
    			sendMessage(PROGRESS_MSG,String.format("%f", printOffset/fileLength),cmd);
    	}else{
    		m = _printStatusFailed.matcher(info);
    		if( m.find() ){
    			_sdstatusThread = null; //stop M27 thread
    			completeCmd();
    			sendMessage(OK_DIALOG,getString(R.string.m27title),getString(R.string.m27info));
    			return;
    		}
	    	if( _list.getCount()>256 ){
	    		for( int i = 0;i < 128;++i )
	    			_list.remove(_list.getItem(i));
	    	}
	    	_list.add(String.format("%s (%s)",cmd,info));
    	}
    }
    @Override
    public void onDestroy(){
    	if( !isFinishing() ){
    		//temporarily detroy?
    	}else{
    		//finish()
    		if( _in != null ){
    			try{_in.close();}catch(Exception e){}
    			_in = null;
    		}
    		_printPause = false;
    		_sdstatusThread = null;
    		_printThread = null;
    		if( mode == 1 ){
    		}else if( mode == 2 ){
    			cmdBuffer("M24");
    		}
    	}
    	mode = 0;
    	super.onDestroy();
    }
}
