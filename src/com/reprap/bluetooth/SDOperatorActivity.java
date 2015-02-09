package com.reprap.bluetooth;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.util.Log;
import android.graphics.Color;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SDOperatorActivity extends ReceiveActivity  {
	private void listFile(){
		cmdSum("M20");
	}
	private void initSD(){
		cmdSum("M21");
	}
	private void releaseSD(){
		cmdSum("M22");
	}	
	private void selectSDFile(String file){
		//cmdSum(String.format("M32 %s",file));
	}
	private void deleteSDFile(String file){
		cmdSum(String.format("M30 %s",file));
	}
	public static final int SDPRINT = 0;
	public static final int BLUETOOTHPRINT = 1;
	private ListView _fileList;
	private ArrayAdapter<String> _list;
	private String _selectFile;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdcard_interface);
        _list = new ArrayAdapter<String>( this,android.R.layout.simple_list_item_1);
        Button delete = (Button)findViewById(R.id.button1);
        delete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if( _selectFile!=null )
				{
					deleteSDFile(_selectFile);
				}
			}
		});
        Button print = (Button)findViewById(R.id.button2);
        print.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
			}
		});
        _fileList = (ListView)findViewById(R.id.listView1);
        _fileList.setAdapter(_list);
        _fileList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
        	@Override
        	public void onItemClick(AdapterView<?> parent,View view,int position,long id){
        		_selectFile = _list.getItem(position);
        		selectSDFile(_selectFile);
                for(int i=0;i<parent.getCount();i++){
                    View v=parent.getChildAt(i);
                    if (position == i) {
                        v.setBackgroundColor(Color.RED);
                    } else {
                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                }       		
        }}); 
        initSD();
    }
    private int flag = 0;
    private Pattern m30 = Pattern.compile("M30[\\s\\S]*");
    @Override
    public void cmdResult(int tag,int state,String info){
    	String cmd = getLastCmdOrigin();
    	
    	if( cmd.compareTo("M20")==0 && info.compareTo("Begin file list")==0){
    		flag = 1;
    		_list.clear();
    	}else if( cmd.compareTo("M20")==0 && info.compareTo("End file list")==0){
    		flag = 0;
    	}else if( flag == 1 && cmd.compareTo("M20")==0 ){
    		_list.add(info);
    	}else if( cmd.compareTo("M21")==0 && tag==STATE_TAG && state==CMD_OK ){
    		listFile();
    	}else{
    		Matcher m = m30.matcher(cmd);
    		if( m.find()&& state == CMD_OK ){
    			listFile();
    		}else{
    			Log.d("ERROR",String.format("Last Command :%s", cmd));
    			Log.d("ERROR",String.format("%d %d %s",tag,state,info));
    		}
    	}
    }
    @Override
    public void onDestroy(){
    	releaseSD();
    	super.onDestroy();
    }
}
