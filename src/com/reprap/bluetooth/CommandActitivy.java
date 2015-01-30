package com.reprap.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

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
		showResult(new String(line,0,line.length));
	}
	private void showResult(String s){
		_text1.setText(_text2.getText());
		_text2.setText(_text3.getText());
		_text3.setText(s);
	}
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.command_interface);
        initAllEvent();
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
    }
}
