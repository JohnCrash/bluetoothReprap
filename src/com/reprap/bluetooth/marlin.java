package com.reprap.bluetooth;

import java.util.regex.Pattern;

public class marlin implements gcode{
	public boolean result(String cmd,String info){
		/*
		 * 没有命令名称直接返回true
		 */
		if( cmd == null )
			return true;
		/*
		 * 这里我假设默认命令返回ok结束
		 */		
		if(info.equals("ok"))
			return true;
		/*
		 * M30 返回
		 */
		if( cmd.equals("M30"))
			return true;
		/*
		 * 不能处理的命令
		 */
		if( info.startsWith("Unknown command:") )
			return true;
		return false;
	}
}
