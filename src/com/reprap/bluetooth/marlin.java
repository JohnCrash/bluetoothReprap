package com.reprap.bluetooth;

import java.util.regex.Pattern;

public class marlin implements gcode{
	public boolean result(String cmd,String info){
		/*
		 * û����������ֱ�ӷ���true
		 */
		if( cmd == null )
			return true;
		/*
		 * �����Ҽ���Ĭ�������ok����
		 */		
		if(info.equals("ok"))
			return true;
		/*
		 * M30 ����
		 */
		if( cmd.equals("M30"))
			return true;
		/*
		 * ���ܴ��������
		 */
		if( info.startsWith("Unknown command:") )
			return true;
		return false;
	}
}
