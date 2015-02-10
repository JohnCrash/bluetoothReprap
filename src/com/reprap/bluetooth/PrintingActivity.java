package com.reprap.bluetooth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

public class PrintingActivity extends ReceiveActivity{
	private String _printFile; 
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
	private int mode = 0;
	private TextView _progressText;
	private TextView _resultText;
	private ProgressBar _progress;
	private void onClick( int id,Button button ){
		switch(id){
		case cont:
			break;
		case pause:
			break;
		case estop:
			break;
		case stop:
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
        _printFile = getIntent().getStringExtra("file");
        if( _printFile != null ){
        	//file print
        	mode = 0;
        }else{
        	//sd print
        	mode = 1;
        }
    }
    @Override
    public void cmdResult( String cmd,String info ){
    
    }
}
