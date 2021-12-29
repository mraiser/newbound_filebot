JSONObject jo = new JSONObject();
File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "botd.properties");
Properties p = BotUtil.loadProperties(f);
jo.put("searchindex", p.getProperty("searchindex", "false").equals("true"));
jo.put("indexcontent", p.getProperty("indexcontent", "false").equals("true"));

File workdir;
if (p.getProperty("indexworkdir") != null) workdir = new File(p.getProperty("indexworkdir"));
else workdir = new File(root, ".index");
jo.put("indexworkdir", workdir.getCanonicalPath());

return jo;