package com.reprap.bluetooth;

public interface gcode {
	public static final int OK = 0;
	public static final int ERROR = -1;
	public static final int RESEND = -2;
	public static final int ECHO = 1;
	public static final int INFO = 2;
	/*
	 * ������ͳ�ȥ������һϵ�н��
	 * ����������ڴ��Ľ�β����true,���򷵻�false
	 */
	public int result(String cmd,String info);
}
