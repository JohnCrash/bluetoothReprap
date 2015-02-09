package com.reprap.bluetooth;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

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
					selectSDFile(_selectFile);
			}
		});
        Button print = (Button)findViewById(R.id.button1);
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
        }});        
        initSD();
    }
    @Override
    public void cmdResult(int tag,int state,String info){
    	String cmd = getLastCmdOrigin();
    	if( cmd == "M20" ){
    		_list.add(info);
    	}else if( cmd == "M21" && tag==STATE_TAG && state==CMD_OK ){
    		listFile();
    	}
    }
    @Override
    public void onDestroy(){
    	releaseSD();
    	super.onDestroy();
    }
}
