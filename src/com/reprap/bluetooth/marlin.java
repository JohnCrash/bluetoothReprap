package com.reprap.bluetooth;

public class marlin implements gcode{
	
	public int result(String cmd,String info){
		/*
		 * û����������ֱ�ӷ���true
		 */
		if( cmd == null )
			return ECHO;
		/*
		 * echo:
		 */
		if( info.startsWith("echo:") )
			return ECHO;
		/*
		 * Error:
		 */
		if( info.startsWith("Error:"))
			return ERROR;
		/*
		 * �����Ҽ���Ĭ�������ok����
		 */		
		if(info.endsWith("ok"))
			return OK;
		return INFO;
	}
}
