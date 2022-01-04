JSONObject jo = new JSONObject();
File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "botd.properties");
Properties p = BotUtil.loadProperties(f);
jo.put("searchindex", p.getProperty("searchindex", "false").equals("true"));
jo.put("indexcontent", p.getProperty("indexcontent", "false").equals("true"));
jo.put("indexcompression", Double.parseDouble(p.getProperty("indexcompression", "1")));
jo.put("indexmaxfilesize", Integer.parseInt(p.getProperty("indexmaxfilesize", ""+(5 * 1024 * 1024))));
jo.put("indexcharset", p.getProperty("indexcharset", "abcdefghijklmnopqrstuvwxyz0123456789.-_"));

File workdir;
if (p.getProperty("indexworkdir") != null) workdir = new File(p.getProperty("indexworkdir"));
else workdir = new File(root, ".index");
jo.put("indexworkdir", workdir.getCanonicalPath());

return jo;