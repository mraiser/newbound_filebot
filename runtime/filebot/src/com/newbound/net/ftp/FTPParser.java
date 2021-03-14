package com.newbound.net.ftp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import com.newbound.net.service.Parser;
import com.newbound.net.service.Request;
import com.newbound.net.service.Response;
import com.newbound.net.service.ServerSocket;
import com.newbound.net.service.Service;
import com.newbound.net.service.Socket;
import com.newbound.net.tcp.TCPServerSocket;
import com.newbound.net.tcp.TCPSocket;
import com.newbound.p2p.P2PConnection;
import com.newbound.robot.BotUtil;
import com.newbound.robot.Callback;
import com.newbound.robot.FileBot;

public class FTPParser implements Parser 
{
	private static final int DEAD = -1;
	private static final int AUTH = 0;
	private static final int TRANS = 1;
	private static final int RNFR = 2;

	Service S;
	Socket SOCK;
	BufferedReader IS = null;
	OutputStream OS = null;

	int STATE = DEAD;
	JSONArray GROUPS = null;
	String WD = "/";
	String username = null;
	int dataSocketPort = -1;
	String dataSocketAddr;
	Socket dataSocket = null;
    Runnable pasvThread = null;
    boolean pasvWait = false;
    File fileHolder = null;
    String fileNameHolder = null;
    
    FileBot mFileBot = null;

    @Override
	public boolean init(Service service, Socket sock) throws Exception 
	{
		S = service;
		SOCK = sock;
		IS = new BufferedReader(new InputStreamReader(SOCK.getInputStream()));
		OS = SOCK.getOutputStream();
		
		mFileBot = (FileBot)S.CONTAINER;
		
		STATE = AUTH;
		send_response(220, "Newbound.io FTP Server v2.0 Control Connection Established");
		
		return true;
	}

	private void send_response(int i, String string) throws IOException 
	{
		OS.write(build_response(i, string+"\r\n").getBytes());
		OS.flush();
	}

	private String build_response(int i, String string) 
	{
		String s = ""+i+" "+string;
		System.out.println("<<< "+s);
		return s;
	}
    
	@Override
	public void send(Response response) throws Exception 
	{
		FTPResponse res = (FTPResponse)response;
		send_response(res.CODE, res.MSG);
	}

	@Override
	public void close() throws Exception 
	{
		SOCK.close();
	}

	@Override
	public void error(Exception x) 
	{
		System.err.println("FTP SERVICE ERROR: "+x);
	}

	@Override
	public Request parse() throws Exception 
	{
		String oneline = IS.readLine();
		return new FTPRequest(oneline);
	}

	@Override
	public void execute(Request data, Callback cb) throws Exception 
	{
		String result = handleCommand((String)data.getCommand());
		if (result != null) OS.write((result+"\r\n").getBytes());
	}

	public String handleCommand(String cmd) throws Exception 
	{
		System.out.println(">>> "+cmd);
		
		String c = cmd.toLowerCase();
		if (STATE == AUTH)
		{
			if (c.startsWith("user ")) return handleUSER(cmd.substring(5));
			if (c.startsWith("pass ")) return handlePASS(cmd.substring(5));
		}
		if (STATE == TRANS)
		{
			if (c.equals("pwd")) return handlePWD();
			if (c.equals("syst")) return handleSYST();
			if (c.startsWith("port ")) return handlePORT(cmd.substring(5));
			if (c.startsWith("eprt ")) return handleEPRT(cmd.substring(5));
			if (c.equals("list")) return handleLIST();
			if (c.equals("nlst")) return handleNLST();
			if (c.startsWith("cwd ")) return handleCWD(cmd.substring(4));
			if (c.startsWith("type ")) return handleTYPE(cmd.substring(5));
			if (c.startsWith("retr ")) return handleRETR(cmd.substring(5));
			if (c.startsWith("stor ")) return handleSTOR(cmd.substring(5));
			if (c.equals("pasv") || c.equals("epsv")) return handlePASV(c);
			if (c.startsWith("rnfr ")) return handleRNFR(cmd.substring(5));
			if (c.startsWith("dele ")) return handleDELE(cmd.substring(5));
			if (c.startsWith("rmd ")) return handleRMD(cmd.substring(4));
			if (c.startsWith("mkd ")) return handleMKD(cmd.substring(4));
			if (c.startsWith("cdup ")) return handleCDUP();
		}
		if (STATE == RNFR)
		{
			if (c.startsWith("rnto ")) return handleRNTO(cmd.substring(5));
		}
		
		return handleBadCommand(cmd);
	}

	private String handleRNTO(String fileName) 
	{
        STATE = TRANS;
    	
		if (isLocal(fileName))
		{
			File oFile = fileHolder;
			fileHolder = null;
			fileNameHolder = null;

			File theFile = parseFileName(fileName);
			
			if (oFile == null)
			{
				return build_response(550, "File not found");
			}

	        // New Filename must be in same directory as Old Filename!
	        try
	        {
	            if (!oFile.getParentFile().equals(theFile.getParentFile()))
	            {
	                return build_response(553, "Invalid file name");
	            }
	        }
	        catch (Exception x)
	        {
	            return build_response(553, "Invalid file name");
	        }

			oFile.renameTo(theFile);

			String s = oFile.getName();
			String s2 = theFile.getName();
			return build_response(250, "OK "+s+" renamed to: "+s2);
		}
		else
		{
			String opath = resolvePath(fileNameHolder);
			String path = resolvePath(fileName);
			
			fileHolder = null;
			fileNameHolder = null;

			// New Filename must be in same directory as Old Filename!
	        try
	        {
	            if (!path.startsWith("/remote/") || !opath.substring(0, opath.lastIndexOf('/')).equals(path.substring(0, path.lastIndexOf('/'))))
	            {
	                return build_response(553, "Invalid file name");
	            }
	            
	            path = path.substring(8);
	            opath = opath.substring(8);
	            
	            int i = path.indexOf('/');
	            String id = path.substring(0,i);
	            path = path.substring(i);
	            opath = opath.substring(i);
	            
	            Hashtable h = new Hashtable();
	            h.put("opath", opath);
	            h.put("path", path);
	            JSONObject jo = mFileBot.sendCommand(id, "filebot", "rnto", h);
	            if (jo.getString("status").equals("ok"))
	            {
	            	return build_response(250, "OK "+opath+" renamed to: "+path);
	            }
	            return build_response(553, "Invalid file name");
	        }
	        catch (Exception x)
	        {
	            return build_response(553, "Invalid file name");
	        }
		}
	}

	private String resolvePath(String fileName) 
	{
		String path;
		if (fileName.startsWith("/")) path = fileName;
		else
		{
			path = WD;
			if (!path.endsWith("/")) path += "/";
			path += fileName;
		}
		return path;
	}

	private boolean isLocal(String fileName) 
	{
		if (fileName.startsWith("/")) return fileName.equals("/local") || fileName.startsWith("/local/");
		return WD.equals("/local") || WD.startsWith("/local/");
	}

	private String handleCDUP() 
	{
		if (WD.equals("/")) return build_response(550, "No such directory");
		
		String wd = WD.substring(0, WD.lastIndexOf('/'));
		if (wd.equals("")) wd = "/";
		
		if (checkPermission(1, wd))
		{
			WD = wd;
			String s = "Directory is: " + BotUtil.replaceString(WD, " ", "%20");
			return build_response(200, s);
		}
		else
		{
			return build_response(550, "Permission denied");
		}
	}

	private String handleMKD(String fileName) 
	{
		String s = fileName;

		if (!checkPermission(2, s))
		{
            return build_response(550, "Permission denied");
		}
		
		if (isLocal(fileName))
		{
			File theFile = parseFileName(fileName);
			theFile.mkdirs();
		}
		else
		{
			try
			{
				String path = resolvePath(fileName);
	            if (!path.startsWith("/remote/"))
	            {
					return build_response(550, "Permission denied");
	            }
	            
	            path = path.substring(8);
	            
	            int i = path.indexOf('/');
	            String id = path.substring(0,i);
	            path = path.substring(i);
	            
	            Hashtable h = new Hashtable();
	            h.put("path", path);
	            JSONObject jo = mFileBot.sendCommand(id, "filebot", "createfolder", h);
	            if (jo.getString("status").equals("ok"))
	            {
	            	return build_response(250, "OK "+path+" created");
	            }
				return build_response(550, "Permission denied");
			}
			catch (Exception x)
			{
				return build_response(550, "Permission denied");
			}
		}
        return build_response(257, "Created Directory "+s);
	}

	private String handleRMD(String fileName) 
	{
		if (!checkPermission(2, fileName))
		{
			return build_response(550, "Permission denied");
		}
		
		if (isLocal(fileName))
		{
			File theFile = parseFileName(fileName);
			if (!theFile.isDirectory())
			{
				return build_response(550, fileName+" is not a directory");
			}

			if (!theFile.delete())
			{
	            String s = fileName;
				return build_response(550, "Unable to remove directory "+s);
			}

			return build_response(250, "Removed Directory: "+fileName);
		}
		else
		{
			try
			{
				String path = resolvePath(fileName);
	            if (!path.startsWith("/remote/"))
	            {
					return build_response(550, "Permission denied");
	            }
	            
	            path = path.substring(8);
	            
	            int i = path.indexOf('/');
	            String id = path.substring(0,i);
	            path = path.substring(i);
	            
	            Hashtable h = new Hashtable();
	            h.put("path", path);
	            JSONObject jo = mFileBot.sendCommand(id, "filebot", "deletefolder", h);
	            if (jo.getString("status").equals("ok"))
	            {
	    			return build_response(250, "Removed Directory: "+fileName);
	            }
				return build_response(550, "Permission denied");
			}
			catch (Exception x)
			{
				return build_response(550, "Permission denied");
			}
		}
	}

	private String handleDELE(String fileName) 
	{

		if (!checkPermission(2, fileName))
		{
            return build_response(550, "Permission denied");
		}

		if (isLocal(fileName))
		{
			File theFile = parseFileName(fileName);
	        if (!theFile.delete())
			{
	            return build_response(550, "Unable to delete file "+fileName);
			}

	        return build_response(250, "Deleted File: "+fileName);
		}
		else
		{
			try
			{
				String path = resolvePath(fileName);
	            if (!path.startsWith("/remote/"))
	            {
					return build_response(550, "Permission denied");
	            }
	            
	            path = path.substring(8);
	            
	            int i = path.indexOf('/');
	            String id = path.substring(0,i);
	            path = path.substring(i);
	            
	            Hashtable h = new Hashtable();
	            h.put("path", path);
	            JSONObject jo = mFileBot.sendCommand(id, "filebot", "deletefile", h);
	            if (jo.getString("status").equals("ok"))
	            {
			        return build_response(250, "Deleted File: "+fileName);
	            }
				return build_response(550, "Permission denied");
			}
			catch (Exception x)
			{
				return build_response(550, "Permission denied");
			}
		}
	}

	private String handleRNFR(String fileName) 
	{
		if (!checkPermission(2, fileName))
		{
			return build_response(550, "Permission denied");
		}

		if (isLocal(fileName))
		{
			File theFile = parseFileName(fileName);
			fileHolder = theFile;
			fileNameHolder = null;
		}
		else
		{
			fileHolder = null;
			fileNameHolder = fileName;
		}
		
		STATE = RNFR;
		return build_response(350, "Target is OK, please send new file name.");
	}

	private String handlePASV(String c) 
	{
        if (pasvThread != null)
        {
            return build_response(421, "Already listening for connection!");
        }
        
        dataSocket = null;	    
        try
        {
//            InetAddress la = InetAddress.getByName(getLocalAddress());
            ServerSocket ss = new TCPServerSocket(0);
//            String addr = la.getHostAddress();
            
            class SockWaiter implements Runnable
            {
//                FTPMachine mMachine = null;
                ServerSocket mSS = null;
                public SockWaiter(ServerSocket ss) { mSS = ss; }
                public void run() 
                { 
                    pasvWait = false;
                    
                    try
                    {
                        dataSocket = mSS.accept();
                        mSS.close();
                    }
                    catch (IOException e) {}
                    finally { pasvThread = null; } 
                } 
            };
            
            pasvThread = new SockWaiter(ss);
            pasvWait = true;
            new Thread(pasvThread).start();
            while (pasvWait) try { Thread.currentThread().sleep(500); } catch (Exception x) { /*IGNORE */ }
    
            if (c.equals("pasv"))
            {
	            int portl = ss.getLocalPort();
	            int porth = portl >> 8;
	            portl = portl & 255;
	    
	            Vector rcv = BotUtil.stringToVector(ss.getInetAddress().getHostAddress(), ".");
	            rcv.addElement(new Integer(porth).toString());
	            rcv.addElement(new Integer(portl).toString());
	            return build_response(227, "Entering Passive Mode ("+BotUtil.vectorToString(rcv, ",")+")");
            }
            else
            {
            	return build_response(229, "Entering Extended Passive Mode (|||"+ss.getLocalPort()+"|)");
            }
        }
        catch (IOException e)
        {
            return build_response(421, "Internal Server Error");
        }
	}

	private String handleSTOR(String fileName) 
	{
        InputStream is = null;

		if (!checkPermission(2, fileName))
		{
			return build_response(550, "Permission denied");
		}

		try 
		{ 
			if (dataSocket == null)
			{
				send_response(150, "STOR OK, Opening data connection");
				dataSocket = new TCPSocket(dataSocketAddr, dataSocketPort); 
			}
			else
			{
				send_response(125, "STOR OK, Data connection already open, sending data");
			}
			is = dataSocket.getInputStream();
			
			if (isLocal(fileName))
			{
				File theFile = parseFileName(fileName);
				if (theFile.isDirectory())
				{
					return build_response(550, "Can't replace a directory with a file.");
				}

				theFile.delete();

				FileOutputStream fos = new FileOutputStream(theFile);
	            BotUtil.sendData(is, fos, -1, 5120);
		
				fos.flush();
				fos.close();
			}
			else
			{
				String path = resolvePath(fileName);
	            if (!path.startsWith("/remote/"))
	            {
					return build_response(550, "Permission denied");
	            }
	            
	            path = path.substring(8);
	            
	            int i = path.indexOf('/');
	            String id = path.substring(0,i);
	            path = path.substring(i);
	            
	            final P2PConnection c = mFileBot.newStream(id);
	            final InputStream fis = is;
	            final boolean[] ba = { false };
	            
	            Runnable r = new Runnable() 
	            {
					public void run() 
					{
						try
						{
			            	OutputStream os = c.getOutputStream();
				            BotUtil.sendData(fis, os, -1, 5120);
				            os.flush();
				            os.close();
				            c.close();
						}
						catch (Exception x) 
						{
							// FIXME should probably do something.
							x.printStackTrace();
						}

			            ba[0] = true;
					}
				};
				new Thread(r).start();
	            
	            Hashtable h = new Hashtable();
	            h.put("stream", c.getID());
	            h.put("path", path);
	            JSONObject jo = mFileBot.sendCommand(id, "filebot", "stor", h);
	            
	            while (!ba[0]) try { Thread.sleep(100); } catch (Exception x) { x.printStackTrace(); }
	            
	            if (jo.getString("status").equals("ok"))
	            {
		            return build_response(250, "Deleted File: "+fileName);
	            }
				return build_response(550, "Permission denied");
			}
			
			try { dataSocket.close(); } catch (Exception x) {/* IGNORE */}
			dataSocket = null;
		}
		catch (Exception e)
		{
			dataSocket = null;

			return build_response(425, "Can't open data connection");
		}
//		machine.log(machine.P_NOTICE, "Received file "+TNS_Sys.getVirtualPath(machine.ftpRoot, theFile));

        return build_response(226, "STOR Data Received");
	}

	private String handleRETR(String fileName) 
	{
		if (!checkPermission(1, fileName))
		{
			return build_response(550, "Permission denied");
		}

		try 
		{ 
			if (dataSocket == null)
			{
				send_response(150, "RETR OK, Opening data connection");
				dataSocket = new TCPSocket(dataSocketAddr, dataSocketPort); 
			}
			else
			{
				send_response(125, "RETR OK, Data connection already open, sending data");
			}

			if (isLocal(fileName))
			{
				try
				{
					File theFile = parseFileName(fileName);
	
					if (theFile == null || (!theFile.exists()) || (theFile.isDirectory()))
					{
						return build_response(550, "No such file");
					}
	
					FileInputStream fis = new FileInputStream(theFile);
		            OutputStream os = dataSocket.getOutputStream();
		            BotUtil.sendData(fis, os, (int)theFile.length(), 5120);
	
					fis.close();
					os.flush();
					os.close();
				}
				catch (Exception x) { return build_response(425, "Error sending file"); }
			}
			else
			{
				try
				{
					String path = resolvePath(fileName);
		            if (!path.startsWith("/remote/"))
		            {
						return build_response(550, "Permission denied");
		            }
		            
		            path = path.substring(8);
		            
		            int i = path.indexOf('/');
		            String id = path.substring(0,i);
		            path = path.substring(i);
		            
		            final P2PConnection c = mFileBot.newStream(id);
		            final boolean[] ba = { false };
		            
		            Runnable r = new Runnable() 
		            {
						public void run() 
						{
							try
							{
				            	InputStream is = c.getInputStream();
					            OutputStream os = dataSocket.getOutputStream();
					            BotUtil.sendData(is, os, -1, 5120);
					            c.close();
					        	
								os.flush();
								os.close();
							}
							catch (Exception x) 
							{
								//FIXME probably should do something here.
								x.printStackTrace();
							}

							ba[0] = true;
						}
					};
					new Thread(r).start();
					
		            Hashtable h = new Hashtable();
		            h.put("stream", c.getID());
		            h.put("path", path);
		            JSONObject jo = mFileBot.sendCommand(id, "filebot", "retr", h);
		            
		            while (!ba[0]) try { Thread.sleep(100); } catch (Exception x) { x.printStackTrace(); }

		            if (!jo.getString("status").equals("ok")) return build_response(550, "Permission denied");
				}
				catch (Exception e)
				{
					return build_response(425, "Can't open data connection");
				}
			}
		}
		catch (IOException e)
		{
			return build_response(425, "Can't open data connection");
		}
		finally
		{
			try { dataSocket.close(); } catch (Exception x) {} 
			dataSocket = null;
		}
		
//		String s = TNS_Sys.getVirtualPath(machine.ftpRoot, theFile);
//		machine.log(machine.P_NOTICE, "Sent File "+s);

        return build_response(226, "RETR Data Sent");
	}

	private File parseFileName(String fileName) 
	{
		if (!fileName.startsWith("/"))
		{
			fileName = WD +(WD.endsWith("/") ? "" : "/")+fileName;
		}
		
		if (fileName.startsWith("/local/")) 
		{
			String path = fileName.substring(6);
			return mFileBot.getLocalFile(path);
		}
		
		return null;
	}

	private String handleTYPE(String cmd) 
	{
		boolean ok = false;
		
		String[] cmds = cmd.split(" ");
		String arg1 = cmds[0];
		String arg2 = cmds.length == 1 ? null : cmds[1];
        
		if (arg1.equals("I")) ok = true;
		else if (arg1.equals("A"))
		{
			if (arg2 == null) ok = true;
			else if (arg2.equals("N")) ok = true;
		}

		if (!ok)
		{
			return build_response(504, "Command TYPE not implemented for that parameter");
		}

		return build_response(200, "TYPE OK");
	}

	private String handleSYST() 
	{
		return build_response(215, "UNIX TYPE: L8");
	}

	private String handleCWD(String newpath) 
	{
		newpath = BotUtil.hexDecode(newpath);
		
		if (checkPermission(1, newpath))
		{
			if (newpath.startsWith("/")) WD = newpath;
			else
			{
				if (!WD.endsWith("/")) WD += "/";
				WD += newpath;
			}
			return build_response(250, "Directory is: "+BotUtil.replaceString(newpath, " ", "%20"));
		}
		else
        {
			return build_response(550, "Permission denied");
        }
	}

	private String handleNLST() 
	{
		if (checkPermission(1, WD))
		{
	        try 
	        { 
	            if (dataSocket == null)
	            {
	                send_response(150, "NLST OK, Opening data connection");
	                dataSocket = new TCPSocket(dataSocketAddr, dataSocketPort); 
	            }
	            else
	            {
	                send_response(125, "NLST OK, Data connection already open, sending data");
	            }
	    
	            DataOutputStream dsos = new DataOutputStream(dataSocket.getOutputStream());
	    
	            if (WD.equals("/"))
	            {
	            	dsos.write(("local\r\n").getBytes());
	            	dsos.write(("remote\r\n").getBytes());
	            }
	            else if (WD.equals("/local") || WD.startsWith("/local/"))
	            {
		            String[] files = parseFileName(WD).list();
		            int i = 0;
		            for (i=0; i<files.length; i++) dsos.write((files[i]+"\r\n").getBytes());
	            }
	            else if (WD.equals("/remote") || WD.startsWith("/remote/"))
	            {
	            	String path = WD.substring(7);
	            	if (path.equals("")) path = "/";
	            	if (path.equals("/"))
	            	{
            			JSONObject o = (JSONObject)mFileBot.sendCommand("peerbot", "connections", new Hashtable());
            			if (o.has("data"))
            			{
    		    	        o = o.getJSONObject("data");
    		    	        
            				Iterator i = o.keys();
            				while (i.hasNext())
            				{
            					String key = (String)i.next();
            					JSONObject peer = o.getJSONObject(key);
//	    		    	        dsos.write((s+" "+(peer.getString("name"))+"\r\n").getBytes());
	    		    	        dsos.write((key+"\r\n").getBytes());
            				}
            			}
	            	}
	            	else
	            	{
	            		while (path.startsWith("/")) path = path.substring(1);
	            		int i = path.indexOf('/');
	            		String peer = i == -1 ? path : path.substring(0,i);
	            		path = i == -1 ? "/" : path.substring(i);
	            		
	            		Hashtable params = new Hashtable();
	            		params.put("path", path);
	            		params.put("children", "true");
	            		JSONObject o = mFileBot.sendCommand(peer, "filebot", "fileinfo", params);
			            if (o.has("list"))
			            {
			            	JSONArray data = o.getJSONArray("list");
			            	int n = data.length();
			            	for (i=0; i<n; i++)
			            	{
			            		JSONObject p = data.getJSONObject(i);
			            		dsos.write((p.getString("id")+"\r\n").getBytes());
			            	}
			            }
	            	}
	            }

	            dsos.flush();
	            dsos.close();
	            dataSocket.close();
	            dataSocket = null;
	        }
	        catch (Exception e)
	        {
	            return build_response(425, "Can't open data connection");
	        }
	    
	        return build_response(226, "NLST Data Sent");
		}
		else
        {
			return build_response(550, "Permission denied");
        }
	}

	private String handleLIST() 
	{
		if (checkPermission(1, WD))
		{
	        try 
	        { 
	            if (dataSocket == null)
	            {
	                send_response(150, "LIST OK, Opening data connection");
	                dataSocket = new TCPSocket(dataSocketAddr, dataSocketPort); 
	            }
	            else
	            {
	                send_response(125, "LIST OK, Data connection already open, sending data");
	            }
	    
	            DataOutputStream dsos = new DataOutputStream(dataSocket.getOutputStream());
	    
	            if (WD.equals("/"))
	            {
	    	        String s = "drwxrwxrwx    1 "+username+"  staff ";
//	    	        String s = "drwxr-xr-x   2 Newbound ";
	    	        s += BotUtil.padStringFront("2", " ", 10)+" ";
	    	        long now = System.currentTimeMillis();
	    	        String uDate = formatDate(now);
	    	        s += uDate; //"May  8 14:06";
	    	        
	    	        dsos.write((s+" local\r\n").getBytes());
	    	        dsos.write((s+" remote\r\n").getBytes());
	            }
	            else if (WD.equals("/local") || WD.startsWith("/local/"))
	            {
	            	String path = WD.substring(6);
	            	if (path.equals("")) path = "/";
		            JSONObject o = mFileBot.handleFileInfo("fileinfo", path, "true");
		            if (o.has("list"))
		            {
		            	JSONArray data = o.getJSONArray("list");
		            	int n = data.length();
		            	for (int i=0; i<n; i++)
		            	{
		            		dsos.write((makeFileStr(data.getJSONObject(i))+"\r\n").getBytes());
		            	}
		            }
	            }
	            else if (WD.equals("/remote") || WD.startsWith("/remote/"))
	            {
	            	String path = WD.substring(7);
	            	if (path.equals("")) path = "/";
	            	if (path.equals("/"))
	            	{
            			JSONObject o = (JSONObject)mFileBot.sendCommand("peerbot", "connections", new Hashtable());
            			if (o.has("data"))
            			{
            		        String s = "drwxrwxrwx    1 "+username+"  staff ";
//    		    	        String s = "drwxr-xr-x   2 Newbound ";
    		    	        s += BotUtil.padStringFront("2", " ", 10)+" ";
    		    	        long now = System.currentTimeMillis();
    		    	        String uDate = formatDate(now);
    		    	        s += uDate; //"May  8 14:06";
    		    	        
    		    	        o = o.getJSONObject("data");
    		    	        
            				Iterator i = o.keys();
            				while (i.hasNext())
            				{
            					String key = (String)i.next();
            					JSONObject peer = o.getJSONObject(key);
//	    		    	        dsos.write((s+" "+(peer.getString("name"))+"\r\n").getBytes());
	    		    	        dsos.write((s+" "+key+"\r\n").getBytes());
            				}
            			}
	            	}
	            	else
	            	{
	            		while (path.startsWith("/")) path = path.substring(1);
	            		int i = path.indexOf('/');
	            		String peer = i == -1 ? path : path.substring(0,i);
	            		path = i == -1 ? "/" : path.substring(i);
	            		
	            		Hashtable params = new Hashtable();
	            		params.put("path", path);
	            		params.put("children", "true");
	            		JSONObject o = mFileBot.sendCommand(peer, "filebot", "fileinfo", params);
			            if (o.has("list"))
			            {
			            	JSONArray data = o.getJSONArray("list");
			            	int n = data.length();
			            	for (i=0; i<n; i++)
			            	{
			            		dsos.write((makeFileStr(data.getJSONObject(i))+"\r\n").getBytes());
			            	}
			            }
	            	}
	            }
//	            else
//	            {
//		            String[] files = getPath(WD).list();
//		            int i = 0;
//		            for (i=0; i<files.length; i++) dsos.write((makeFileStr(new File(getPath(WD), files[i]))+"\r\n").getBytes());
//	            }
	            
	            dsos.flush();
	            dsos.close();
	            dataSocket.close();
	            dataSocket = null;
	        }
	        catch (Exception e)
	        {
	            return build_response(425, "Can't open data connection");
	        }
	    
	        return build_response(226, "LIST Data Sent");

		}
		return build_response(550, "Permission denied");
	}

    /**
     * Generate a UNIX style file description.
     * @return java.lang.String
     * @param f java.io.File
     */
    public String makeFileStr(JSONObject f) throws Exception
    {
    
        // PERMISSION
        String s = "rwxrwxrwx    1 "+username+"  staff ";
    
        // TYPE
        if (f.getBoolean("directory")) s = "d"+s+" ";
        else s = "-"+s+" ";
    
        // SIZE
        s += BotUtil.padStringFront(""+f.getInt("size"), " ", 10)+" ";
    
        // DATE
        String uDate = formatDate(f.getLong("modified"));
//        Vector vDate = stringToVector(uDate, " ");
//        vDate.removeElementAt(2);
//        uDate = vectorToString(vDate, " ");
        s += uDate;
        
//        s += " " + replaceString(f.getString("name"), " ", "%20");
        s += " " + f.getString("name");
    
        System.out.println(s);
        
        return s;
    }

	public String formatDate(long millis)
	{
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millis);
		String s = Calendar.getInstance().get(Calendar.YEAR) == c.get(Calendar.YEAR) ? "kk:mm" : "yyyy";
		return new SimpleDateFormat("MMM dd "+s).format(new Date(millis));
	}

	private boolean checkPermission(int i, String wD2) 
	{
		return true;
	}

	private String handlePORT(String portinfo) 
	{
		try
		{
			int i = 0;
			int j = 0;

			if ((i=portinfo.indexOf("(")) != -1)
			{
				if ((j=portinfo.indexOf(")")) == -1) throw new IOException();
				if (j<i) throw new IOException();
				portinfo = portinfo.substring(i+1, j);
			}

			String[] portV = portinfo.split(",");
			if (portV.length < 6) throw new IOException();

			i = Integer.parseInt((String)portV[4]);
			j = Integer.parseInt((String)portV[5]);
			dataSocketPort = (i << 8) + j;

			dataSocketAddr = portV[0]+"."+portV[1]+"."+portV[2]+"."+portV[3];

			return build_response(200, "PORT Acknowledged");
		}
		catch (IOException e) { return build_response(501, "Wrong parameters for PORT"); }
		catch (NumberFormatException e) { return build_response(501, "Wrong parameters for PORT"); }
	}

	private String handleEPRT(String portinfo) 
	{
		try
		{
			while (portinfo.startsWith("|")) portinfo = portinfo.substring(1);
			while (portinfo.endsWith("|")) portinfo = portinfo.substring(0, portinfo.length()-1);
			int i = portinfo.indexOf('|');
			String type = portinfo.substring(0,i);
			portinfo = portinfo.substring(i+1);
			i = portinfo.indexOf('|');
			dataSocketAddr = portinfo.substring(0,i);
			portinfo = portinfo.substring(i+1);
//			portinfo = portinfo.substring(portinfo.lastIndexOf('|')+1);
			dataSocketPort = Integer.parseInt(portinfo);

			return build_response(200, "EPRT Acknowledged");
		}
		catch (NumberFormatException e) { return build_response(501, "Wrong parameters for EPRT"); }
	}

	private String handlePWD() 
	{
        return build_response(257, "\""+WD+"\" is the current directory");
	}

	private String getCurrentWD() 
	{
		return WD;
	}

	private String handlePASS(String pass) 
	{
		try
		{
			GROUPS = mFileBot.checkPassword(username, pass);
			if (GROUPS.length() == 1 && GROUPS.getString(0).equals("anonymous")) return build_response(530, "Invalid username or password");
			else 
			{
				STATE = TRANS;
				return build_response(230, "User logged in.");
			}
		}
		catch (Exception x) { x.printStackTrace(); }
		
		return handleBadCommand(pass);
	}

	private String handleUSER(String cmd) 
	{
		username = cmd;
		return build_response(331, "Password required to access user account "+username+".");
	}

	private String handleBadCommand(String cmd) 
	{
		return build_response(500, "Invalid Command "+cmd);
	}
	
}
