package com.reprap.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.os.Message;
import android.widget.LinearLayout;
import android.content.res.Configuration;
import android.util.Log;
import android.os.Handler;

public class CommandActitivy extends ReceiveActivity {
	static final int g28 = R.id.button2;
	static final int m18 = R.id.button3;
	static final int fan = R.id.button10;
	static final int extracter1 = R.id.button5;
	static final int extracter2 = R.id.button1;
	static final int stop = R.id.button11;
	static final int sd = R.id.button7;
	static final int carlidration = R.id.button6;
	static final int console = R.id.button4;
	static final int [] button_id = {g28,m18,fan,extracter1,extracter2,stop,
		sd,carlidration,console};
	static final long TIME_OUT = 1000;
	static final int COMMAND = 0;
	TextView _hot1Text;
	TextView _fanText;
	TextView _extracter1Text;
	TextView _extracter2Text;
	TextView _text1;
	TextView _text2;
	TextView _text3;
	Button _hotButton;
	Button _fanButton;
	Button _extractor1Button;
	Button _extractor2Button;
	LinearLayout _linear1;
	LinearLayout _linear2;
	Handler _handler;
	/*
	 * 从一个命令发出开始计时
	 */
	long _cmdTime = 0;
	protected void initAllEvent(){
		for( int i = 0;i < button_id.length;i++ ){
			final int id = button_id[i];
			findViewById(id).setOnClickListener(new View.OnClickListener(){
	        	@Override
	        	public void onClick(View view){
	        		CommandActitivy.this.onClick(id);
	        	}
	        });
		}
	}
	private void cmd( String s ){
		long current = System.currentTimeMillis();
		if( _cmdTime == 0 || current-_cmdTime>TIME_OUT ){
			writeString(s+"\r\n");
			_cmdTime = current;
		}else{
			/*
			 * 放入列表或者忽略
			 */
			Log.d(TAG,"上一个命令还没有返回.");
		}
	}
	private void onClick( int id ){
		switch(id){
		case g28:
			cmd("G28");
			break;
		case m18:
			cmd("M18");
			break;
		case fan:
			break;
		case extracter1:
			break;
		case extracter2:
			break;
		case sd:
			{
				Intent intent = new Intent(CommandActitivy.this,SDOperatorActivity.class);
				if( intent != null ){
					startActivity(intent);
					//CommandActitivy.this.startActivityFromFragment(CommandActitivy.this,intent,0);
				}  
			}
			break;
		case carlidration:
			{
				Intent intent = new Intent(CommandActitivy.this,CalibrationActivity.class);
				if( intent != null ){
					startActivity(intent);
				}
			}
			break;
		case console:
			{
				Intent intent = new Intent(CommandActitivy.this,ConselActivity.class);
				if( intent != null ){
					startActivity(intent);
				}
			}
			break;
		}
	}
	@Override
	public void receiver( byte [] line ){
		_cmdTime = 0;
		showResult(new String(line,0,line.length));
	}
	private void showResult(String s){
		_text1.setText(_text2.getText());
		_text2.setText(_text3.getText());
		_text3.setText(s);
	}
	private View [] _buts;
	private int but_state = 0;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.command_interface);
        initAllEvent();
        /*
         * 在横屏的时候屏幕比较长，将按钮放置在一个tablerow中
         */
        _linear1 = (LinearLayout)findViewById(R.id.linear1);
        _linear2 = (LinearLayout)findViewById(R.id.linear2);
        _buts = new View[_linear2.getChildCount()];
        for(int i =0;i<_linear2.getChildCount();++i){
        	_buts[i] = _linear2.getChildAt(i);
        }
        but_state = 1;
        _hot1Text = (TextView)findViewById(R.id.textView1);
        _fanText = (TextView)findViewById(R.id.textView2);
        _extracter1Text = (TextView)findViewById(R.id.textView3);
        _extracter2Text = (TextView)findViewById(R.id.textView4);
        _hotButton = (Button)findViewById(R.id.button9);
        _fanButton = (Button)findViewById(fan);
        _extractor1Button = (Button)findViewById(extracter1);
        _extractor2Button = (Button)findViewById(extracter2);
        _text1 = (TextView)findViewById(R.id.textView5); 
        _text2 = (TextView)findViewById(R.id.textView6); 
        _text3 = (TextView)findViewById(R.id.textView7); 
        UIOrentation();
        InitMonitoringThread();
    }
    private static String TAG = "INFO";
    private void UIOrentation(){
    	//判断方向
	     if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
	    	 if( but_state == 1 ){
	    		 for( int i = 0;i<_buts.length;i++){
	    			 _linear2.removeView(_buts[i]);
	    			 _linear1.addView(_buts[i]);
	    		 }
	    	 }
	    	 but_state = 2;
	     }
	     else {
	    	 if( but_state == 2 ){
	    		 for( int i = 0;i<_buts.length;i++){
	    			 _linear1.removeView(_buts[i]);
	    			 _linear2.addView(_buts[i]);
	    		 }	    	 
	    	 }
	    	 but_state = 1;
	     }    	
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    { 
        super.onConfigurationChanged(newConfig); 
        UIOrentation();
    }
    private Thread _monitoringThread;
    /*
     * 启动一个监视线程，用于监视打印机的实时温度等参数
     */
    private void InitMonitoringThread(){
    	_handler = new Handler(){
    		@Override
    		public void handleMessage(final Message msg){
    			if( msg.what == COMMAND )
    				cmd((String)msg.obj);
    		}
    	};
    	_monitoringThread = new Thread(){
    		@Override
    		public void run(){
    			while( _monitoringThread == Thread.currentThread() ){
    				sendMessage(COMMAND,"M105");
    				try{
    				Thread.sleep(1000);
    				}catch(Exception e){}
    			}
    		}
    	};
    	_monitoringThread.start();
    }
    private void sendMessage( int id,String cmd ){
    	if( _handler == null )return;
		Message msg = new Message();
		msg.what = id;
		msg.obj = cmd;
		_handler.sendMessage(msg);
    }
    @Override
    public void onDestroy(){
    	_monitoringThread = null;
    	super.onDestroy();
    }
}
