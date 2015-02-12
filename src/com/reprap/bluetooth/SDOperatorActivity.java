package com.reprap.bluetooth;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.util.Log;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.app.Dialog; 

public class SDOperatorActivity extends ReceiveActivity  {
	private void listFile(){
		cmdBuffer("M20");
	}
	private void initSD(){
		cmdBuffer("M21");
	}
	private void releaseSD(){
		cmdBuffer("M22");
	}	
	private void selectSDFile(String file){
		//cmdSum(String.format("M32 %s",file));
	}
	private void deleteSDFile(String file){
		cmdBuffer(String.format("M30 %s",file));
	}
	///上传文件到reprap
	private void startUpload( String f ){
		
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
				Intent intent = new Intent(SDOperatorActivity.this,PrintingActivity.class);
				if( intent != null ){
					startActivity(intent);
				} 				
			}
		});
        Button upload = (Button)findViewById(R.id.button3);
        upload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//从手机上传数据到reprap
				Map<String,Integer> images = new HashMap<String,Integer>();
	            images.put(FileChooser.sRoot, R.drawable.filedialog_root);   // 根目录图标  
	            images.put(FileChooser.sParent, R.drawable.filedialog_folder_up);    //返回上一层的图标  
	            images.put(FileChooser.sFolder, R.drawable.filedialog_folder);   //文件夹图标  
	            images.put("gco", R.drawable.filedialog_wavfile);   //gco文件图标  
	            images.put(FileChooser.sEmpty, R.drawable.filedialog_root);
	            String gcpath = Environment.getExternalStorageDirectory().getPath()+"/gcode";
	            /*
	             * 在扩展存储上创建一个目录gcode
	             */
	            java.io.File f = new java.io.File(gcpath);
	            if(!f.exists())
	            	f.mkdir();
	            FileChooser.sRoot = gcpath+"/";            
				Dialog dialog = FileChooser.createDialog(SDOperatorActivity.this,getString(R.string.gcode_chooser),
						new CallbackBundle(){
					@Override
					public void callback(Bundle bundle){
						String filepath = bundle.getString("path");		
						/*
						 * 这里启动一个上传线程
						 */
						startUpload( filepath );
					}
					
				},".gco;.gcode;",images);
				dialog.show();				
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
    public void cmdResult(String cmd,String info){
    	if( cmd == null ){
    		Log.d("ERROR","cmdResult cmd = null");
    		Log.d("ERROR",String.format("info=%d",info));
    		return;
    	}
    	Matcher mok = okPattern.matcher(info);
    	if( cmd.compareTo("M20")==0 && info.compareTo("Begin file list")==0){
    		flag = 1;
    		_list.clear();
    	}else if( cmd.compareTo("M20")==0 && info.compareTo("End file list")==0){
    		flag = 0;
    	}else if( flag == 1 && cmd.compareTo("M20")==0 ){
    		_list.add(info.toLowerCase());
    	}else if( cmd.compareTo("M21")==0 && mok.find() ){
    		listFile();
    	}else{
    		Matcher m = m30.matcher(cmd);
    		if( m.find() ){
    			completeCmd(); //命令已经识别,没有ok
    			listFile();
    		}else{
    			Log.d("ERROR",String.format("Last Command :%s", cmd));
    			Log.d("ERROR",info);
    		}
    	}
    }
    @Override
    public void onDestroy(){
    	releaseSD();
    	super.onDestroy();
    }
}
