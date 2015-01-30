package com.reprap.bluetooth;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class CommandActitivy extends ReceiveActivity {
	static final int g28 = R.id.button2;
	static final int fan = R.id.button10;
	static final int extracter1 = R.id.button5;
	static final int extracter2 = R.id.button1;
	static final int [] button_id = {g28,fan,extracter1,extracter2};
	TextView _hot1Text;
	TextView _fanText;
	TextView _extracter1Text;
	TextView _extracter2Text;
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
		writeString(s+"\r\n");
	}
	private void onClick( int id ){
		switch(id){
		case g28:
			cmd("G28");
			break;
		}
	}
	@Override
	public void receiver( byte [] line ){
		
	}
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.command_interface);
        initAllEvent();
        _hot1Text = (TextView)findViewById(R.id.textView1);
        _fanText = (TextView)findViewById(R.id.textView2);
        _extracter1Text = (TextView)findViewById(R.id.textView3);
        _extracter2Text = (TextView)findViewById(R.id.textView4);
    }
}
