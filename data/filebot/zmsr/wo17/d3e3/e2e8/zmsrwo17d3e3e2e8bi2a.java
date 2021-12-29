File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "botd.properties");
Properties p = BotUtil.loadProperties(f);
p.setProperty("searchindex", ""+searchindex);
p.setProperty("indexcontent", ""+indexcontent);
p.setProperty("indexworkdir", indexworkdir);
BotUtil.storeProperties(p, f);

return "OK";