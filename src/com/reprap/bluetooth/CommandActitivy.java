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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.widget.SeekBar;

public class CommandActitivy extends ReceiveActivity {
	static final int hot1temp = R.id.button9;
	static final int g28 = R.id.button2;
	static final int m18 = R.id.button3;
	static final int fan = R.id.button10;
	static final int reset = R.id.button11;
	static final int extracter1 = R.id.button5;
	static final int extracter2 = R.id.button1;
	static final int extracter1back = R.id.button13;
	static final int extracter2back = R.id.button12;
	static final int stop = R.id.button11;
	static final int sd = R.id.button7;
	static final int carlidration = R.id.button6;
	static final int console = R.id.button4;
	static final int [] button_id = {g28,m18,fan,extracter1,extracter2,stop,
		sd,carlidration,console,hot1temp,reset,extracter1back,extracter2back};
	static final long TIME_OUT = 1000;
	static final int COMMAND = 0;
	TextView _hot1Text;
	TextView _hot2Text;
	SeekBar _hot1Bar;
	SeekBar _hot2Bar;
	TextView _fanText;
	SeekBar _fanBar;
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
	protected void initAllEvent(){
		for( int i = 0;i < button_id.length;i++ ){
			final int id = button_id[i];
			findViewById(id).setOnClickListener(new View.OnClickListener(){
	        	@Override
	        	public void onClick(View view){
	        		CommandActitivy.this.onClick(id,(Button)view);
	        	}
	        });
		}
	}
	int max_temperature = 230; //hot1 max temperature
	int min_temperature = 160;
	int hot1temperature = 180; //hot1 target temperature
	int hot2temperature = 180;
	int fanValue = 100;
	private void onClick( int id,Button button ){
		switch(id){
		case g28: //原点
			cmdSum("G28");
			break;
		case m18: //停止电机
			cmdSum("M18");
			break;
		case reset://紧急停止
			cmdSum("M112");
			break;
		case hot1temp: //热头1温度
			{
				String heating = getString(R.string.heating);
				String stop = getString(R.string.stop);
				if( button.getText() == heating ){
					cmdSum(String.format("M104 T0 S%d",hot1temperature));
					button.setText(stop);
				}else if( button.getText() == stop ){
					cmdSum(String.format("M104 T0 S0"));
					button.setText(heating);
				}else{
					button.setText(heating);
				}
			}
			break;
		case fan: //风扇
			{
				String running = getString(R.string.fan_isrunning);
				String stop = getString(R.string.stop);
				if( button.getText() == running ){
					cmdSum(String.format("M106 S%d", fanValue*255/100));
					button.setText(stop);
				}else if( button.getText() == stop ){
					cmdSum(String.format("M107"));
					button.setText(running);
				}else{
					button.setText(running);
				}
			}
			break;
		case extracter1: //挤出机1挤出
			break;
		case extracter1back:
			break;
		case extracter2:
			break;
		case extracter2back:
			break;			
		case sd: //SD卡操作界面
			{
				Intent intent = new Intent(CommandActitivy.this,SDOperatorActivity.class);
				if( intent != null ){
					startActivity(intent);
					//CommandActitivy.this.startActivityFromFragment(CommandActitivy.this,intent,0);
				}  
			}
			break;
		case carlidration: //校准打印机
			{
				Intent intent = new Intent(CommandActitivy.this,CalibrationActivity.class);
				if( intent != null ){
					startActivity(intent);
				}
			}
			break;
		case console: //控制台
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
	public void cmdResult( int tag,int state,String info ){
		showResult(info);
	}
	private Pattern _terp;
	private void initRegex(){
		_terp = Pattern.compile("ok T:(\\d*.*\\d*)/(\\d*.*\\d*) B:(\\d*.*\\d*)/(\\d*.*\\d*) @:(\\d*.*\\d*) B@:(\\d*.*\\d*)");
	}
	private String _m105t1;
	private String _m105t2;
	private String _m105t3;
	private String _m105t4;
	private void showhot1(int i){
		String format = getString(R.string.hot_format);
		String text;
		if( i == 1 ){
			text = String.format(format, i,_m105t1,_m105t2,hot1temperature);
			_hot1Text.setText(text);
		}else if( i == 2){
			text = String.format(format, i,_m105t3,_m105t4,hot2temperature);
			_hot2Text.setText(text);			
		}
	}
	private void showfan(){
		String format = getString(R.string.fan_format);
		String text = String.format(format, fanValue);
		_fanText.setText(text);
	}
	private void showResult(String s){
		_text1.setText(_text2.getText());
		_text2.setText(_text3.getText());
		_text3.setText(s);
		String cmd = getLastCmdOrigin();
		if( cmd != null && cmd != "M105" )
			setTitle(title+cmd+"("+s+")");
		else if( cmd == "M105" ){
			//温度反馈，像这样的格式 ok T:25.1/0.0 B:4.0/0.0 @:0 B@:0
			Matcher m = _terp.matcher(s);
			while(m.find()){
				_m105t1 = m.group(1); //当前温度
				_m105t2 = m.group(2); //目标温度
				_m105t3 = m.group(3);
				_m105t4 = m.group(4);
				showhot1(1);
			}
		}
	}
	private View [] _buts;
	private int but_state = 0;
	private String title;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.command_interface);
        title = this.getString(R.string.title_command);
        initAllEvent();
        initRegex();
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
        _hot2Text = (TextView)findViewById(R.id.textView8);
        _hot1Bar = (SeekBar)findViewById(R.id.seekBar1); //hot1 temperature
        _hot1Bar.setMax(max_temperature-min_temperature);
        _hot1Bar.setProgress(hot1temperature-min_temperature);
        _hot1Bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        	@Override
        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
        		hot1temperature = progress+min_temperature;
        		showhot1(1);
        	}
        	@Override
        	public void onStartTrackingTouch(SeekBar seekBar){
        	}
        	@Override
        	public void onStopTrackingTouch(SeekBar seekBar){
        	}
        });
        _hot2Bar = (SeekBar)findViewById(R.id.seekBar5); //hot2 temperature
        _hot2Bar.setMax(max_temperature-min_temperature);
        _hot2Bar.setProgress(hot1temperature-min_temperature);
        _hot2Bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        	@Override
        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
        		hot2temperature = progress+min_temperature;
        		showhot1(2);
        	}
        	@Override
        	public void onStartTrackingTouch(SeekBar seekBar){
        	}
        	@Override
        	public void onStopTrackingTouch(SeekBar seekBar){
        	}
        });
        _fanText = (TextView)findViewById(R.id.textView2);
        _fanBar = (SeekBar)findViewById(R.id.seekBar2);
        _fanBar.setMax(100);
        _fanBar.setProgress(fanValue);
        _fanBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        	@Override
        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
        		fanValue = progress;
        		showfan();
        	}
        	@Override
        	public void onStartTrackingTouch(SeekBar seekBar){
        	}
        	@Override
        	public void onStopTrackingTouch(SeekBar seekBar){
        		/*
        		 * 如果已经开启了风扇，设置结束时直接调整风扇的转速
        		 */
        		String stop = getString(R.string.stop);
        		if( CommandActitivy.this._fanButton.getText() == stop ){
        			cmdSum(String.format("M106 S%d", fanValue*255/100));
        		}
        	}
        });
        _extracter1Text = (TextView)findViewById(R.id.textView3);
        _extracter2Text = (TextView)findViewById(R.id.textView4);
        _hotButton = (Button)findViewById(R.id.button9);
        _fanButton = (Button)findViewById(fan);
        _fanButton.setText(getString(R.string.fan_isrunning));
        _extractor1Button = (Button)findViewById(extracter1);
        _extractor2Button = (Button)findViewById(extracter2);
        _text1 = (TextView)findViewById(R.id.textView5); 
        _text2 = (TextView)findViewById(R.id.textView6); 
        _text3 = (TextView)findViewById(R.id.textView7); 
        UIOrentation();
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
    				cmdSum((String)msg.obj);
    		}
    	};
    	_monitoringThread = new Thread(){
    		@Override
    		public void run(){
    			while( _monitoringThread == Thread.currentThread() ){
    				if( !sendMessage(COMMAND,"M105") )return;
    				try{
    					Thread.sleep(1000);
    				}catch(Exception e){return;}
    			}
    		}
    	};
    	_monitoringThread.start();
    }
    private boolean sendMessage( int id,String cmd ){
    	if( _handler == null )return false;
		Message msg = new Message();
		msg.what = id;
		msg.obj = cmd;
		_handler.sendMessage(msg);
		return true;
    }
    @Override
    public void onStop(){
    	_monitoringThread = null;
    	super.onStop();
    }
    @Override
    public void onStart(){
    	super.onStart();
    	InitMonitoringThread();
    }
    @Override
    public void onDestroy(){
    	_monitoringThread = null;
    	_handler = null;
    	super.onDestroy();
    }
}
