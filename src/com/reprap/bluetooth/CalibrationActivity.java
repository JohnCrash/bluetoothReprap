package com.reprap.bluetooth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CalibrationActivity extends ReceiveActivity  {
	static final int g28 = R.id.button8;
	static final int g29 = R.id.button1;
	static final int m329 = R.id.button2;
	static final int estop = R.id.button9;
	static final int save = R.id.button3;
	static final int m114 = R.id.button4;
	static final int [] button_id = {g28,g29,m329,estop,save,m114};
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jiaozhun_interface);
        setTitle(getString(R.string.calibration_title));
        initAllEvent();
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
			cmdBuffer("M500");
			break;
		case m114:
			cmdBuffer("M114");
			break;
		}
	}
	public void cmdResult( String cmd,String result ){
		
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
}
