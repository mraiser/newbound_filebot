package com.newbound.net.ftp;

import java.io.IOException;

import com.newbound.net.service.Container;
import com.newbound.net.service.ServerSocket;
import com.newbound.net.service.Service;
import com.newbound.net.tcp.TCPServerSocket;
import com.newbound.robot.FileBot;

public class FTPService extends Service {

	public FTPService(FileBot app, int port) throws IOException 
	{
		super(new TCPServerSocket(port), "FTP", FTPParser.class, app);
	}

}
