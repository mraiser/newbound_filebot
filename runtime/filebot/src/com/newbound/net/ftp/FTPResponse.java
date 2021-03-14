package com.newbound.net.ftp;

import com.newbound.net.service.Response;

public class FTPResponse implements Response 
{
	int CODE;
	String MSG;
	
	public FTPResponse(int code, String msg)
	{
		CODE = code;
		MSG = msg;
	}
}
