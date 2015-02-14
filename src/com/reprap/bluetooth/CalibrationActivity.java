package com.reprap.bluetooth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class CalibrationActivity extends ReceiveActivity  {
	static final int g28 = R.id.button8;
	static final int g29 = R.id.button1;
	static final int m329 = R.id.button2;
	static final int estop = R.id.button9;
	static final int save = R.id.button3;
	static final int down100 = R.id.button5;
	static final int down50 = R.id.button6;
	static final int down = R.id.button4;
	static final int up = R.id.button7;
	SeekBar seekBar;
	TextView text;
	private RadioButton radios[] = new RadioButton[4];
	static final int [] button_id = {g28,g29,m329,estop,save,down100,down50,down,up};
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        	setContentView(R.layout.jiaozhun_interface2);
        else
        	setContentView(R.layout.jiaozhun_interface);
        setTitle(getString(R.string.calibration_title));
        initAllEvent();
        text = (TextView)findViewById(R.id.textView2);
        seekBar = (SeekBar)findViewById(R.id.seekBar1);
        text.setText("5mm");
        seekBar.setProgress(50);
        setText(50);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
        	@Override
        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
        		setText(progress);
        	}
        	@Override
        	public void onStartTrackingTouch(SeekBar seekBar){
        	}
        	@Override
        	public void onStopTrackingTouch(SeekBar seekBar){
        	}
        });
        radios[0] = (RadioButton)findViewById(R.id.radioButton1);
        radios[1] = (RadioButton)findViewById(R.id.radioButton2);
        radios[2] = (RadioButton)findViewById(R.id.radioButton3);
        radios[3] = (RadioButton)findViewById(R.id.radioButton4);
        startM119Thread();
    }
    private float z = 200;
    void setText(int progress){
    	text.setText(String.format("move %#.1fmm Z=%#.1f",(float)progress/10,z));
    }
	private void onClick( int id,Button button ){
		switch(id){
		case g28:
			cmdBuffer("G28");
			break;
		case g29:
			cmdBuffer("G29");
			break;
		case m329:
			cmdBuffer("M329");
			break;
		case estop:
			cmdBuffer("M112");
			break;
		case save:
			/*
			 * 将打印头正好接触打印面然后存储.
			 * 先发出M501确定M206默认值
			 */
		{
			cmdBuffer("M501");
			break;
		}
		case down100:
			cmdBuffer("G1 Z100 F8000");
			z = 100;
			setText(seekBar.getProgress());
			break;
		case down50:
			cmdBuffer("G1 Z50 F8000");
			z = 50;
			setText(seekBar.getProgress());
			break;
		case down:
		{
			z -= (float)seekBar.getProgress()/10;
			setText(seekBar.getProgress());
			cmdBuffer(String.format("G1 Z%#.1f F8000",z));
			break;
		}
		case up:
		{
			z += (float)seekBar.getProgress()/10;
			setText(seekBar.getProgress());
			cmdBuffer(String.format("G1 Z%#.1f F8000",z));
			break;
		}
		}
	}
	@Override
	public void cmdResult( String cmd,String result,int s ){
		if( cmd.equals("M119") ){
			if( result.startsWith("x_max:") ){
				if(result.endsWith("open") ){
					radios[0].setChecked(false);
				}else{
					radios[0].setChecked(true);
				}
			}
			if( result.startsWith("y_max:") ){
				if(result.endsWith("open") ){
					radios[1].setChecked(false);
				}else{
					radios[1].setChecked(true);
				}
			}
			if( result.startsWith("z_max:") ){
				if(result.endsWith("open") ){
					radios[2].setChecked(false);
				}else{
					radios[2].setChecked(true);
				}
			}
			if( result.startsWith("z_min:") ){
				if(result.endsWith("open") ){
					radios[3].setChecked(false);
				}else{
					radios[3].setChecked(true);
				}
			}
		}else if( cmd.equals("M501")){
			if(result.startsWith("echo:  M206") ){
				Pattern p = Pattern.compile("echo:  M206 X([-\\d.]+) Y([-\\d.]+) Z([-\\d.]+)");
				Matcher m = p.matcher(result);
				if( m.find() ){
					float hz = Float.parseFloat(m.group(3));
					text.setText(String.format("M206 Z%#.1f",hz-z));
					cmdBuffer(String.format("M206 Z%#.1f",hz-z));
					cmdBuffer("M500");
				}
			}
		}
	}	
	protected void initAllEvent(){
		for( int i = 0;i < button_id.length;i++ ){
			final int id = button_id[i];
			findViewById(id).setOnClickListener(new View.OnClickListener(){
	        	@Override
	        	public void onClick(View view){
	        		CalibrationActivity.this.onClick(id,(Button)view);
	        	}
	        });
		}
	}	
	/*
	 * 
	 */
	Thread _m119Thread;
	void startM119Thread(){
		if( _m119Thread != null )return; 
		_m119Thread = new Thread(){
			@Override
			public void run(){
				Thread thisThread = Thread.currentThread();
				while(thisThread==_m119Thread){
					cmdBuffer("M119");
					try{sleep(1000);}catch(Exception e){
						_m119Thread = null;
					}
				}
			}
		};
		_m119Thread.start();
	}
	@Override
	public void onDestroy(){
		_m119Thread = null;
		super.onDestroy();
	}
}
