package com.newbound.net.ftp;

import org.json.JSONObject;

import com.newbound.net.service.Request;

public class FTPRequest implements Request 
{
	String CMD;
	
	public FTPRequest(String cmd) 
	{
		CMD = cmd;
	}

	@Override
	public Object getCommand() 
	{
		return CMD;
	}

	@Override
	public JSONObject getData() 
	{
		JSONObject jo = new JSONObject();
		try { jo.put("cmd", CMD); } catch (Exception x) { x.printStackTrace(); }
		return jo;
	}

}
