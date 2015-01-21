package com.reprap.bluetooth;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.app.Activity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.ArrayAdapter;

public class ConselActivity extends Activity {
	private EditText _input;
	private ArrayAdapter<String> _list;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consol_layout);
        
        _input = (EditText)findViewById(R.id.editText1);
        ListView lv = (ListView)findViewById(R.id.listView1);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        lv.setAdapter(_list);
        _input.setOnEditorActionListener(new OnEditorActionListener(){
        	@Override
        	 public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
        		String cmd = v.getText().toString();
        		v.setText("");
        		_list.add(cmd);
        		return true;
        	}
        });
    }
}
