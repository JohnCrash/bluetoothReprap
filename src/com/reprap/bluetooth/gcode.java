package com.reprap.bluetooth;

public interface gcode {
	public static final int OK = 0;
	public static final int ERROR = -1;
	public static final int RESEND = -2;
	public static final int ECHO = 1;
	public static final int INFO = 2;
	/*
	 * 命令被发送出去，返回一系列结果
	 * 如果是命令期待的结尾返回true,否则返回false
	 */
	public int result(String cmd,String info);
}
