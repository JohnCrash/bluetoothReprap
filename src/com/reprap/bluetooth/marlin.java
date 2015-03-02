package com.reprap.bluetooth;

public class marlin implements gcode{
	
	public int result(String cmd,String info){
		/*
		 * 没有命令名称直接返回true
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
		 * Resend:
		 */
		if( info.startsWith("Resend: "))
			return RESEND;
		/*
		 * 这里我假设默认命令返回ok结束
		 */		
		if(info.endsWith("ok"))
			return OK;
		if( cmd.equals("M105") && info.startsWith("ok T:"))
			return OK;
		if( cmd.equals("M29") && info.endsWith("Done saving file."))
			return OK;
		return INFO;
	}
}
