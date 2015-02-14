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

import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

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
	private ProgressDialog uploadDialog = null;
	private String _uploadFile = null;
	private Thread _uploadThread = null;
	private BufferedInputStream _in = null;
	private long fileLength = 0;
	private boolean isRawMode = false;
	private boolean waitClear = false;
	private void startUpload( String f ){
		File file = new File(f);
		try{
			fileLength = file.length();
			_in = new BufferedInputStream(new FileInputStream(file));
		}catch(Exception e){
			Log.d("ERROR","Can not open file "+f);
			return;
		}
		if( fileLength <= 0){
			Log.d("ERROR","File size = 0, "+f);
			return;
		}
		/*
		 * wait for reprap buffer is empty
		 */
		int waitcount = 0;
		while(!isCmdBufferEmpty()){
			try{Thread.sleep(100);}catch(Exception e){}
			if( waitcount++ > 50 ){
				Log.d("ERROR","Wait buffer empty,time out");
				return;
			}
		}		
		_uploadFile = f;
		uploadDialog = new ProgressDialog(this);
		uploadDialog.setTitle(getString(R.string.upload));
		uploadDialog.setMessage(String.format(getString(R.string.upload_format),f));
		uploadDialog.setIcon(R.drawable.ic_launcher);
		uploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		uploadDialog.setProgress(0);
		uploadDialog.setCancelable(false);
		uploadDialog.show();
		_uploadThread = new Thread(){
			@Override
			public void run(){
				long offset = 0;
				Thread thisThread = Thread.currentThread();
				String fname = to83Format(_uploadFile.toLowerCase());
				cmdBuffer("M28 "+fname);
				isRawMode = true;
				waitClear = false;
				while(thisThread==_uploadThread){
					byte [] line = new byte[256];
					int i = 0;
					try{
						do{
							int b = _in.read();
							if( b == -1 ){
								_uploadThread = null;
								break;
							}
							if( b == '\r' )continue;
							if( b == '\n' )
								break;
							line[i++] = (byte)b;
						}while( i < 256 );
						if(i>0){
							offset += (i+2);
							uploadDialog.setProgress((int)((float)(offset*100)/(float)fileLength));
							while(waitClear)
								sleep(10);
							cmdRaw(new String(line,0,i));
							waitClear = true;
						}
					}catch(Exception e){
						Log.d("ERROR",e.getMessage());
						cmdBuffer("M29");
						_in = null;
						_uploadFile = null;
						uploadDialog.cancel();
						uploadDialog = null;
					}
				}
				isRawMode = false;
				cmdBuffer("M29");
				try{_in.close();}catch(Exception e){
					Log.d("ERROR",e.getMessage());
				}
				_in = null;
				_uploadFile = null;
				uploadDialog.cancel();		
				uploadDialog = null;
			}
		};
		_uploadThread.start();
	}
	/*
	 * 将文件名转换为8.3文件名,有路径去掉
	 */
	private String to83Format(String f){
		StringBuffer fn = new StringBuffer();
		for( int i = f.length()-1;i>=0;i--){
			char c = f.charAt(i);
			if( c == '/' || c== '\\' )
				break;
			fn.append(c);
		}
		StringBuffer fn83 = new StringBuffer();
		int l = 0;
		int e = 0;
		int m = 0;
		for( int i = fn.length()-1;i>0;i--){
			char c = fn.charAt(i);
			if(l++ >= 8)
				m = 1;
			if( c == '.' )
				m = 2;
			if( m == 2&&e++>4 )
				break;
			if( m==0 || m == 2)
				fn83.append(c);
		}
		return fn83.toString();
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
    @Override
    public void cmdResult(String cmd,String info,int result){
    	if( isRawMode ){
    		if( info.equals("ok") )
    			waitClear = false;
    		return;
    	}
    	if( cmd.equals("M29") && result ==gcode.OK ){
    		listFile();
    		return;
    	}
    	if( cmd.equals("M20") && info.equals("Begin file list")){
    		flag = 1;
    		_list.clear();
    	}else if( cmd.equals("M20") && info.equals("End file list")){
    		flag = 0;
    	}else if( flag == 1 && cmd.equals("M20") ){
    		_list.add(info.toLowerCase());
    	}else if( cmd.equals("M21") && result==gcode.OK ){
    		listFile();
    	}else if( cmd.startsWith("M30") && result==gcode.OK ){
    		listFile();
    	}
    }
    @Override
    public void onDestroy(){
    	releaseSD();
    	super.onDestroy();
    }
}
