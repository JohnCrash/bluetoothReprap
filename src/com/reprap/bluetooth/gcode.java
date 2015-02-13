package com.reprap.bluetooth;

public interface gcode {
	/*
	 * 命令被发送出去，返回一系列结果
	 * 如果是命令期待的结尾返回true,否则返回false
	 */
	public boolean result(String cmd,String info);
}
