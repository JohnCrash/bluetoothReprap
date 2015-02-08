package com.reprap.bluetooth;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.view.View;
import java.lang.System;

public class ConselActivity extends ReceiveActivity {
	private EditText _input;
	private ArrayAdapter<String> _list;
	private ListView _listView;
	private long _lastCmdTime = 0;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consol_layout);

        setTitle(String.format("Console (%s)",getBluetoothDevice().getName()));
        _input = (EditText)findViewById(R.id.editText1);
        _listView = (ListView)findViewById(R.id.listView1);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        _listView.setAdapter(_list);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
        	@Override
        	public void onItemClick(AdapterView<?> parent,View view,int position,long id){
        		String cmd = _list.getItem(position);
        		int i = cmd.indexOf(" (");
        		if( i != -1 )
        			cmd = cmd.substring(0,i);
        		_lastCmdTime = System.currentTimeMillis();
        		_list.add(cmd);
        		cmd += "\r\n";
        		write( cmd.getBytes() );
        }});
        _input.setOnEditorActionListener(new OnEditorActionListener(){
        	@Override
        	 public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
        		String cmd = v.getText().toString();
        		if(cmd.length()>0){ 
        			v.setText("");
        			_lastCmdTime = System.currentTimeMillis();
        			_list.add(cmd);
     				cmd += "\r\n";
       				write(cmd.getBytes());
        		}
        		return true;
        	}
        });
    }
    @Override
    public boolean receiver( byte [] buffer ){
    	super.receiver( buffer );
    	if( System.currentTimeMillis()-_lastCmdTime < 1000 ){
    		String s = _list.getItem(_list.getCount()-1);
    		String ns = String.format("%s (%s)",s,new String(buffer,0,buffer.length));
    		_list.remove(s);
    		_list.add(ns);
    		_lastCmdTime = 0;
    	}else{
    		_list.add(new String(buffer,0,buffer.length) );
    	}
    	return true;
    }
}
