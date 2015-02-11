package com.reprap.bluetooth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class PrintingActivity extends ReceiveActivity{
	private static String _printFileName = null; 
	private static File _printFile = null;
	private static BufferedInputStream _in = null;
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
	private TextView _progressText;
	private TextView _resultText;
	private ProgressBar _progress;
	private void onClick( int id,Button button ){
		switch(id){
		case cont:
			if( mode == 1 ){
				
			}else if( mode == 2 ){
				cmdBuffer("M24");
			}
			break;
		case pause:
			if( mode == 1 ){
				
			}else if( mode == 2 ){
				cmdBuffer("M25");
			}
			break;
		case estop:
			mode = 0;
			try{_in.close();_in=null;}catch(Exception e){}
			_printThread = null;
			cmdBuffer("M112");
			break;
		case stop:
			if( mode == 1 ){
				
			}else if( mode == 2 ){
				cmdBuffer("M24");
			}
			break;
		}
	}
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.printing_interface);
        initAllEvent();
        _progressText = (TextView)findViewById(R.id.textView1);
        _resultText = (TextView)findViewById(R.id.textView2);
        _progress = (ProgressBar)findViewById(R.id.progressBar1);
        _printFileName = getIntent().getStringExtra("file");
        if( _printFileName != null ){
        	//file print
        	if( mode == 0 ){
        		mode = 1;
	        	try{
	        		_printFile = new File(_printFileName);
	        		_in = new BufferedInputStream(new FileInputStream(_printFile) );
	        		bluetoothPrintThread();
	        	}catch(Exception e){
	        		Log.d("ERROR",e.getMessage());
	        	}
        	}
        }else{
        	//sd print
        	mode = 2;
        }
    }
    Thread _printThread;
    protected void bluetoothPrintThread(){
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
    						if( b == -1 )continue;
    						if( b == '\r')continue;
    						if( b == '\n')
    							break;
    						line[i++] = (byte)b;
    					}while( i < 256 );
    					if( i > 0 ){
    						cmdBuffer(new String(line,0,i));
    						while( isCmdBufferOver() ){
    							try{sleep(50);}catch(Exception e){return;}
    							if( _printThread != thisThread ){return;}
    						}
    					}
    				}catch(Exception e){
    					Log.d("ERROR",e.getMessage());
    				}
    			}
    		}
    	};
    }
    @Override
    public void cmdResult( String cmd,String info ){
    
    }
}
