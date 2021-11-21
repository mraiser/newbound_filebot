package com.newbound.robot;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import com.newbound.net.mime.Base64Coder;
import com.newbound.net.service.http.HTTPService;
import com.newbound.util.DirectoryIndex;
import com.newbound.util.NoDotFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.newbound.net.mime.MIMEMultipart;
import com.newbound.net.service.Socket;

public class FileBot extends MetaBot
{
//	FTPService mFTPServer = null;
	Hashtable<String, DirectoryIndex> mIndex = new Hashtable();
	//boolean mLazyIndex = true;
	boolean mIndexContent = true;
	
	private FilenameFilter nodots = new FilenameFilter() 
	{
		public boolean accept(File dir, String name) { return !name.startsWith("."); }
	};

	protected Properties mShared = null;
	
	public FileBot() 
	{
		super();
	}

	public void init() throws Exception 
	{
		super.init();
		try
		{
			loadSharedFiles();
//			if (PROPERTIES.getProperty("startftp", "false").equals("true"))
//				mFTPServer = new FTPService(this, 2626);
			loadSearchIndexes();
		}
		catch (Exception x) { x.printStackTrace(); }
	}

	private void loadSearchIndexes() {
		mIndex.clear();
		if (PROPERTIES.getProperty("searchindex", "false").equals("true"))
		{
			File workdir;

			if (PROPERTIES.getProperty("indexworkdir") != null) workdir = new File(PROPERTIES.getProperty("indexworkdir"));
			else workdir = new File(getRootDir(), ".index");
			mIndexContent = PROPERTIES.getProperty("indexcontent", "true").equals("true");

			File root = getRootDir();
			File f = new File(root, "excludefromsearch.txt");
			String[] excludes = null;
			if (f.exists()) try {
				String s = new String(BotUtil.readFile(f));
				excludes = s.split("\n");
			}
			catch (Exception x) { x.printStackTrace(); }

			Enumeration shares = mShared.propertyNames();
			while (shares.hasMoreElements())
			{
				String share = (String) shares.nextElement();
				String path = mShared.getProperty(share);
				DirectoryIndex di = new DirectoryIndex(
						new File(path),
						workdir,
						new NoDotFilter(),
						"abcdefghijklmnopqrstuvwxyz0123456789.-_",
						(short)3,
						1,
						mIndexContent,
						5 * 1024 * 1024);
				if (excludes != null) di.exclude(excludes);
				mIndex.put(share, di);
			}
		}
	}

	private void loadSharedFiles() throws Exception
	{
		File f = new File(getRootDir(), "fileshare.properties");
		if (f.exists()) mShared = loadProperties(f);
		else 
		{
			mShared = new Properties();
			Hashtable<String,File> h = SYS.getSharedFolders();
			Enumeration<String> e = h.keys();
			while (e.hasMoreElements())
			{
				String k = e.nextElement();
				f = h.get(k);
				f.mkdirs();
				mShared.put(k, f.getCanonicalPath());
			}
			
			f = new File(getRootDir(), "fileshare.properties");
			FileOutputStream fos = new FileOutputStream(f);
			mShared.store(fos, "");
			fos.flush();
			fos.close();
		}
	}

	public Object handleCommand(String cmd, Hashtable params) throws Exception 
	{
		if (cmd.startsWith("local/")) return handleLocal(cmd, params);
		if (cmd.equals("fileinfo")) return handleFileInfo(cmd, params);
		if (cmd.equals("createfolder")) return handleCreateFolder(cmd, params);
		if (cmd.equals("createfile")) return handleCreateFile(cmd, params);
//		if (cmd.equals("setcontent")) return handleSetContent(cmd, params);
//		if (cmd.equals("getcontent")) return handleGetContent(cmd, params);
		if (cmd.equals("deletefile")) return handleDeleteFile(cmd, params);
		if (cmd.equals("deletefolder")) return handleDeleteFolder(cmd, params);
		if (cmd.equals("newfolder")) return handleNewFolder(cmd, params);
		if (cmd.equals("uploadfile")) return handleUploadFile(cmd, params);
		if (cmd.startsWith("zipdir/")) return handleZipDir(cmd, params);
		if (cmd.equals("updatefileshare")) return handleUpdateFileShare(cmd, params);
		if (cmd.equals("listcommonfolders")) return handleListCommonFolders(cmd, params);
		if (cmd.equals("rnto")) return handleRNTO(cmd, params);
		if (cmd.equals("stor")) return handleSTOR(cmd, params);
		if (cmd.equals("retr")) return handleRETR(cmd, params);
		if (cmd.equals("search")) return handleSearch(cmd, params);
		if (cmd.equals("index")) return handleIndex(cmd, params);

		throw new Exception("Unknown command");
	}
	
	public JSONObject getCommands()
	{
		JSONObject commands = new JSONObject();
		JSONObject cmd;
		
		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("desc", "Retrieve a file from a local share.<br><b>usage:</b> http://localhost:5773/filebot/local/sharename/relative/path/filename.ext	");
		commands.put("local", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\",\"children\"]"));
		cmd.put("desc", "Returns metadata about a file. NOTE: default is children=true. If you do not want metadata for content, pass: children=false");
		commands.put("fileinfo", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\"]"));
		cmd.put("desc", "Create new folder at specified path.");
		commands.put("createfolder", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\"]"));
		cmd.put("desc", "Create new empty file at specified path.");
		commands.put("createfile", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\"]"));
		cmd.put("desc", "Delete file at specified path.");
		commands.put("deletefile", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\"]"));
		cmd.put("desc", "Delete folder at specified path and all of its contents.");
		commands.put("deletefolder", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"name\",\"path\"]"));
		cmd.put("desc", "Create new folder with specified name inside the folder at the specified path.");
		commands.put("newfolder", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"name\",\"path\",\"filename\",\"files[]\"]"));
		cmd.put("desc", "Upload a file to the given path. NOTE: Requires enctype=&quot;mime/multipart&quot; in the form tag.");
		commands.put("uploadfile", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\"]"));
		cmd.put("desc", "Returns a ZIP archive of the folder at specified path and all of its contents.<br><b>usage:</b> http://localhost:5773/filebot/zipdir/archive.zip?path=/files/archive");
		commands.put("zipdir", cmd);

		cmd = new JSONObject();
		cmd.put("parameters", new JSONArray("[\"v\"]"));
		cmd.put("desc", "Update the list of directories to share.<br><b>usage:</b> http://localhost:5773/filebot/updatefileshare?v=share1\tpath1\rshare2\tpath2");
		commands.put("updatefileshare", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("desc", "Returns a list of folders on the local device that people often share.");
		commands.put("listcommonfolders", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"opath\",\"path\"]"));
		cmd.put("desc", "Rename the file from opath to path.");
		commands.put("rnto", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"stream\",\"path\",\"sessionid\"]"));
		cmd.put("desc", "Upload a file asynchronusly to the given path with the contents coming from the given stream.");
		commands.put("stor", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"stream\",\"path\",\"sessionid\"]"));
		cmd.put("desc", "Download a file asynchronusly from the given path over the given stream.");
		commands.put("retr", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\",\"query\",\"uuid\"]"));
		cmd.put("desc", "Search the local shared file path for the given query string.");
		commands.put("search", cmd);

		cmd = new JSONObject();
		cmd.put("groups", "trusted,files");
		cmd.put("parameters", new JSONArray("[\"path\"]"));
		cmd.put("desc", "Index the local shared file path for searching.");
		commands.put("index", cmd);

		return commands;
	}
	
/*
	private Object handleGetContent(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		String stream = (String)params.get("stream");
		String id = (String)params.get("sessionid");
		PeerBot pb = PeerBot.getPeerBot();
		final P2PConnection con = pb.getPeer(id).getStream(Long.parseLong(stream));
		final InputStream is = getFileContent(file);
		final int length = (int)getLocalFile(file).length();
		if (is == null) throw new Exception("No such file");
		
		Runnable r = new Runnable() 
		{
			public void run() 
			{
				try
				{
					OutputStream os = con.getOutputStream();
					MIMEMultipart.sendData(is, os, length, 4096);
					os.flush();
					os.close();
					is.close();
					con.close();
				}
				catch (Exception x) { x.printStackTrace(); }
			}
		};
		new Thread(r).start();
		
		JSONObject o = newResponse();
		o.put("length", length);
		
		return o;
	}
*/
	private JSONObject handleIndex(String cmd, Hashtable params) throws Exception
	{
		String path = (String) params.get("path");
		File f = getLocalFile(path);
		if (f == null) throw new Exception("Directory not found: "+path);

		String share = parseShare(path);
		DirectoryIndex di = mIndex.get(share);
		if (di == null) throw new Exception("No index for share: "+share);

		JSONObject jo = newResponse();

		try {
			di.index(f);
		}
		catch (Exception x) {
			jo.put("status", "err");
			jo.put("msg", x.getMessage());
			f = new File(getRootDir(), "directoryindex_error.txt");
			PrintWriter pw = new PrintWriter(new FileWriter(f));
			x.printStackTrace(pw);
			pw.close();
		}

		return jo;
	}

	private JSONObject handleSearch(String cmd, Hashtable params) throws Exception
	{
		JSONObject jo = newResponse();
		String path = (String) params.get("path");
		String query = (String) params.get("query");
		final String uuid = (String)params.get("uuid");

		final String share = parseShare(path);
		final String sharepath = new File(mShared.getProperty(share)).getCanonicalPath();

		DirectoryIndex di = mIndex.get(share);
		if (di == null) throw new Exception("No index for share: "+share);

		File f = getLocalFile(path);
		if (f == null)  throw new Exception("Directory not found: "+share);

		final JSONArray ja = new JSONArray();
		jo.put("list", ja);

		FileVisitor v = new SimpleFileVisitor()
		{
			@Override
			public FileVisitResult visitFile(Object o, BasicFileAttributes basicFileAttributes) throws IOException
			{
				String found = "/"+share+((File)o).getCanonicalPath().substring(sharepath.length());
				if (uuid != null) {
					JSONObject jo = new JSONObject();
					jo.put("msg", found);
					jo.put("guid", uuid);
					//BotBase.getBot(botname).
					sendWebsocketMessage(jo.toString());
				}
				//else
					ja.put(found);
				return null;
			}
		};
		di.search(f, query, v, mIndexContent, false);

		return jo;
	}

	private String parseShare(String path)
	{
		int i,j;
		i = path.startsWith("/") ? 1 : 0;
		j = path.indexOf('/', i);
		if (j == -1) j = path.length();
		String share = path.substring(i,j);
		return share;
	}

	private JSONObject handleRETR(String cmd, Hashtable params) throws Exception
	{
		String stream = (String)params.get("stream");
		String path = (String)params.get("path");
		String uuid = (String)params.get("sessionid");
		File f = getLocalFile(path);
		final Socket c = getStream(uuid, Long.parseLong(stream));
		final FileInputStream is = new FileInputStream(getLocalFile(path));
		final OutputStream os = c.getOutputStream();
		
		Runnable r = new Runnable() 
		{
			public void run() 
			{
				try
				{
					sendData(is, os, -1, 5120);
					os.flush();
					os.close();
					c.close();
				}
				catch (Exception x) { x.printStackTrace(); }
			}
		};
		new Thread(r).start();
		
		return newResponse();
	}
	
	private JSONObject handleSTOR(String cmd, Hashtable params) throws Exception
	{
		String stream = (String)params.get("stream");
		String path = (String)params.get("path");
		String uuid = (String)params.get("sessionid");
		File f = getLocalFile(path);
		final Socket c = getStream(uuid, Long.parseLong(stream));
		final InputStream is = c.getInputStream();
		final FileOutputStream os = new FileOutputStream(getLocalFile(path));
		
		Runnable r = new Runnable() 
		{
			public void run() 
			{
				try
				{
					sendData(is, os, -1, 5120);
					os.flush();
					os.close();
					c.close();
				}
				catch (Exception x) { x.printStackTrace(); }
			}
		};
		new Thread(r).start();
		
		return newResponse();
	}
	
	private JSONObject handleRNTO(String cmd, Hashtable params) throws Exception
	{
		String opath = (String)params.get("opath");
		String path = (String)params.get("path");
		File oldf = getLocalFile(opath);
		File newf = getLocalFile(path);
		if (!oldf.renameTo(newf)) throw new Exception("Unable to rename "+opath+" to "+path);
		
		return newResponse();
	}
	
	private Object handleUploadFile(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("FILEUPDLOAD");
		String path = (String)params.get("path");
		String name = (String)params.get("name");
		name = name.substring(name.lastIndexOf('/')+1);
		name = name.substring(name.lastIndexOf('\\')+1);
		
		File f = getLocalFile(path);
		f = new File(f, name);
		
		if (file == null)
		{
			String s = (String)params.get("files[]");
			if (s == null)
			{
				s = (String)params.get("BASE64");
				byte[] ba = Base64Coder.decode(s);
				writeFile(f, ba);
			}
			else writeFile(f, s.getBytes());
		}
		else
		{
			File f2 = new File(file);
			boolean b = f2.renameTo(f);
		}
		
		JSONObject o = newResponse();
		JSONArray ja = new JSONArray();
		o.put("files", ja);
		JSONObject o2 = new JSONObject();
		o2.put("name", name);
		o2.put("size", f.length());
		ja.put(o2);
		
		return o;
	}

	private Object handleDeleteFile(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		if (!deleteFile(file)) throw new Exception("Cannot delete file");
		return newResponse();
	}

	private Object handleDeleteFolder(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		deleteDir(getLocalFile(file));
		return newResponse();
	}

	private Object handleNewFolder(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		String name = (String)params.get("name");
		new File(getLocalFile(file), name).mkdirs();
		return newResponse();
	}
/*
	private Object handleSetContent(String cmd, Hashtable params) throws Exception 
	{
		final String file = (String)params.get("path");
		String stream = (String)params.get("stream");
		String id = (String)params.get("sessionid");
		final int length = Integer.parseInt((String)params.get("length"));
		PeerBot pb = PeerBot.getPeerBot();
		final P2PConnection con = pb.getPeer(id).getStream(Long.parseLong(stream));
		Runnable r = new Runnable() 
		{
			public void run() 
			{
				try {  setFileContent(file, con.getInputStream(), length); } catch (Exception x) { x.printStackTrace(); }
				try { con.close(); } catch (Exception x) { x.printStackTrace(); }
			}
		};
		new Thread(r).start();
		
		return newResponse();
	}
*/
	private Object handleCreateFile(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		
		if (file == null) throw new Exception("Parameter path is required");
		createFile(file);
		
		return newResponse();
	}

	private Object handleCreateFolder(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		
		if (file == null) throw new Exception("Parameter path is required");
		createFolder(file);
		
		return newResponse();
	}

	private Object handleFileInfo(String cmd, Hashtable params) throws Exception
	{
		String file = (String)params.get("path");
		String kids = (String)params.get("children");
		return handleFileInfo(cmd, file, kids);
	}
	
	public JSONObject handleFileInfo(String cmd, String file, String kids) throws Exception
	{
		JSONObject o = newResponse();
		
		if (file == null) throw new Exception("Parameter path is required");
		
		if (file.equals("/"))
		{
			o.put("name", "/");
			o.put("path", "/");
			o.put("exists", true);
			o.put("directory", true);
			
			Enumeration e = mShared.keys();
			String[] list = new String[mShared.size()];
			int i = 0;
			while (e.hasMoreElements()) list[i++] = (String)e.nextElement();
			if (kids == null || kids.equals("false")) o.put("list", toJSONArray(list));
			else addKids(o, list, file, cmd);

			o.put("size", mShared.size());
			o.put("modified", System.currentTimeMillis());
		}
		else
		{
			int i = file.lastIndexOf('/');
			String name = i == -1 ? file : file.substring(i+1);
			File f = getLocalFile(file);
			if (f == null) 
			{
				o.put("name", name);
				o.put("path", file);
				o.put("exists", false);
				o.put("modified", -1);
				o.put("directory", false);
				o.put("size", -1);
			}
			else
			{
				String path = f.getCanonicalPath();

				o.put("name", name);
				o.put("path", file);
				o.put("realpath", path);
				o.put("exists", f.exists());
				o.put("modified", f.lastModified());
				o.put("directory", f.isDirectory());
				if (f.isDirectory()) 
				{
//					String[] list = f.list(nodots);
					String[] list = f.list();
					ArrayList<String> jv = new ArrayList<String>();
					i = list.length;
					while (i-->0)  if (!list[i].startsWith(".")) jv.add(list[i]);
					Collections.sort(jv);
					list = new String[jv.size()];
					list = jv.toArray(list);
					
					if (kids == null || kids.equals("false")) o.put("list", toJSONArray(list));
					else addKids(o, list, file, cmd);
					o.put("size", list.length);

					String share = parseShare(file);
					DirectoryIndex di = mIndex.get(share);
					long index = di == null ? -1 : di.lastIndex(f);
					o.put("lastindex", index);
				}
				else o.put("size", f.length());
			}
		}
		
		return o;
	}

	private void addKids(JSONObject o, String[] list, String file, String cmd) throws Exception 
	{
		JSONArray ja = new JSONArray();
		o.put("list", ja);						
		int n = list.length;
		int i;
		for (i=0; i<n; i++) 
		{
//			Hashtable p2 = new Hashtable();
			String newfile = file.equals("/") ? "/"+list[i] : file + "/" + list[i];
//			p2.put("path",  newfile);
			ja.put((JSONObject)handleFileInfo(cmd, newfile, null));
		}
	}

	private Object handleLocal(String cmd, Hashtable params) throws Exception
	{
		cmd = URLDecoder.decode(cmd.substring(cmd.indexOf('/')), "UTF-8");
		File f = getLocalFile(cmd);

		if (cmd.equals("/") || f.isDirectory())
		{
			File[] files;

			if (cmd.equals("/"))
			{
				files = new File[mShared.size()];
				Enumeration e = mShared.keys();
				int i = 0;
				while (e.hasMoreElements())
				{
					final String key = (String)e.nextElement();
					final String val = mShared.getProperty(key);
					File f2 = new File(val)
					{
						public String getName()
						{
							return key;
						}
						
						public String getCanonicalPath()
						{
							return val;
						}
					};
					files[i++] = f2;
				}
			}
			else 
			{
//				String[] list = f.list(nodots);
				String[] list = f.list();
				int i=list.length;
				files = new File[i];
				while (i-->0) files[i] = new File(f, list[i]);
			}
			
//			String s = "<html><head><title>"+cmd+"</title></head><body><pre>PATH: <b>"+cmd+"</b><br><br>";
//			for (int i=0; i<files.length; i++) s += getFileLink(files[i], files[i].getName(), null);
//			s += "</pre></body></html>";

			URL u = getResource("html/filebot/index.html");
			InputStream is = (InputStream)u.getContent();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			sendData(is, os, -1, 4096);
			is.close();
			os.flush();os.close();
			String s = os.toString();
			int i = s.indexOf("var curpath = \"/\";");
			if (!cmd.equals("/") && cmd.endsWith("/")) cmd = cmd.substring(0, cmd.length()-1);
			s = s.substring(0,i+15)+cmd+s.substring(i+16);
			
			return s.getBytes();

		}
		else
		{
			if (f.exists() && f.isFile()) return f;
		}
		
		throw new Exception("No such file");
	}

	public File getLocalFile(String cmd) 
	{
		if (cmd.equals("/")) return null;
		
		while (cmd.startsWith("/")) cmd = cmd.substring(1);
		while (cmd.endsWith("/")) cmd = cmd.substring(0, cmd.length()-1);
		int i = cmd.indexOf('/');
		if (i == -1)
		{
			String x = mShared.getProperty(cmd);
			if (x == null) return null;
			return new File(x);
		}
		else
		{
			String dir = cmd.substring(0,i);
			cmd = cmd.substring(i+1);
			File f = new File(mShared.getProperty(dir));
			
			while (true)
			{
				i = cmd.indexOf('/');
				if (i == -1)
				{
					f = new File(f, cmd);
					break;
				}
				else
				{
					String name = cmd.substring(0,i);
					cmd = cmd.substring(i+1);
					f = new File(f, name);
				}
			}
			
			return f;
		}
	}

	private static final String[] sizes = { "b ", "kb", "mb", "gb", "tb", "pb" };
	
	private String getFileLink(File f, String name, String url) throws IOException
	{
		if (f == null) f = new File("/");
		long size = f.length();
		if (url == null) url = getFileURL(f);
		int oom = 0;
		while (size > 1024) 
		{
			size = size / 1024;
			oom++;
		}
		String len = ("        "+size+" "+sizes[oom]);
		len = len.substring(len.length()-7)+"   ";
		String ico = f.isDirectory() ? "folder" : "file";
		
		String s ="<a href='"+url+"' style='text-decoration: none;'><img src='/filebot/img/"+ico+".jpeg' style='vertical-align:text-top;width:16px;height:16px;'> "+len+name+"</a><br>";
		
		return s;
	}
	
	private String getFileURL(File f) throws IOException
	{
		if (f == null) return "";
		String s = f.getName();
		while (s.startsWith("/")) s=s.substring(1); 
		if (f.isDirectory()) s += "/";
		return s;
	}

	public String getServiceName() 
	{
		return "filebot";
	}

	public String getIndexFileName() 
	{
		return "index.html";
	}

	public void createFolder(String path) throws IOException
	{
		File f = getLocalFile(path);
		f.mkdir();
	}

	public void createFile(String path) throws IOException
	{
		System.out.println("Creating file: "+path);
		File f = getLocalFile(path);
		f.createNewFile();
//		f.delete();
	}

	public long setFileContent(String path, InputStream is, int length) throws Exception
	{
		File f = getLocalFile(path);
		FileOutputStream fos = new FileOutputStream(f);
		System.out.println("NOW AVAILABLE: "+is.available());
//		if (is.available() > 0)
			MIMEMultipart.sendData(is, fos, length, 4096);
		fos.flush();
		fos.close();
		return f.length();
	}

	public boolean deleteFile(String path) 
	{
		File f = getLocalFile(path);
		return f.delete();
	}

	public InputStream getFileContent(String path) throws IOException 
	{
		File f = getLocalFile(path);
		if (f == null || !f.exists() || f.isDirectory()) 
			return null;
		
		FileInputStream fis = new FileInputStream(f);
		return fis;
	}
	
	private Object handleZipDir(String cmd, Hashtable params) throws IOException 
	{
		class POS extends PipedOutputStream
		{
			public long COUNT = 0;
			public boolean ALIVE = true;

			public void write(int b) throws IOException 
			{
				super.write(b);
				COUNT++;
			}

			public void write(byte[] b, int off, int len) throws IOException 
			{
				super.write(b, off, len);
				COUNT += len;
			}

			public void close() throws IOException 
			{
				super.close();
				ALIVE = false;
			}
			
		};
		
		final POS pos = new POS();
		
		class PIS extends PipedInputStream
		{
			public long COUNT = 0;
			public POS IPOS = null;
			
			public PIS(POS pos) throws IOException
			{
				super(pos);
				IPOS = pos;
			}

			public synchronized int read() throws IOException 
			{
				if (!IPOS.ALIVE && COUNT == IPOS.COUNT) return -1;
				
				int i = super.read();
				COUNT++;
				
				if (!IPOS.ALIVE && COUNT == IPOS.COUNT) 
					close();
				
				return i;
			}
		};
		
		final PIS pis = new PIS(pos);

		
		final File f = getLocalFile((String)params.get("path"));
		Object[] oa = { pis, -1 };
		
		Runnable r = new Runnable() 
		{
			public void run() 
			{
				try
				{
					
//					File f2 = newTempFile();
//					FileOutputStream fos = new FileOutputStream(f2);
//					zipDir(f, fos); 
//					fos.flush();
//					fos.close();
					zipDir(f, pos); 
					pos.flush();
					pos.close();
					
//					int len = (int)f2.length();
//					oa[1] = len;
//					oa[0] = pis;
					
//					FileInputStream fis = new FileInputStream(f2);
					
//					sendData(fis, pos, len, 4096);
					
					while (pis.COUNT<pos.COUNT) try 
					{ 
						Thread.sleep(100); 
					} 
					catch (Exception x) {}
					
				} catch (Exception x) { x.printStackTrace(); }
			}
		};
		new Thread(r).start();
		
		while (oa[0] == null) try { Thread.sleep(100); } catch (Exception x) { x.printStackTrace(); }
		
		return oa;
	}

	private Object handleUpdateFileShare(String cmd, Hashtable params) throws Exception 
	{
		Properties p = new Properties();
		String s = (String)params.get("v");
		String[] sa = s.split("\r");
		int n = sa.length;
		int i;
		for (i=0; i<n; i++) 
		{
			String[] sa2 = sa[i].split("\t");
			if (sa2.length>1)
			{
				p.setProperty(sa2[0], sa2[1]);
				File f = new File(sa2[1]);
				if (!f.exists() && !f.mkdir()) throw new Exception("Unable to create folder: "+sa[1]);
			}
		}
		File f = new File(getRootDir(), "fileshare.properties");
		storeProperties(p, f);
		mShared = p;

		loadSearchIndexes();
		
		return newResponse();
	}

	public static FileBot getFileBot() 
	{
		return (FileBot)mBots.get("filebot");
	}

	private Object handleListCommonFolders(String cmd, Hashtable params) throws JSONException
	{
		JSONObject out = newResponse();
		out.put("data", new JSONObject(SYS.getCommonFolders()));
		return out;
	}
}
