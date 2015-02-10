package com.reprap.bluetooth;

import android.os.Bundle;

public class PrintingActivity extends ReceiveActivity{
	private String _printFile; 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.printing_interface);

        _printFile = getIntent().getStringExtra("file");
    }
    @Override
    public void cmdResult( String cmd,String info ){
    
    }
}
