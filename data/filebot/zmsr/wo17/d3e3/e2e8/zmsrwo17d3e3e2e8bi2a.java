File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "botd.properties");
Properties p = BotUtil.loadProperties(f);
p.setProperty("searchindex", ""+searchindex);
p.setProperty("indexcontent", ""+indexcontent);
p.setProperty("indexworkdir", indexworkdir);
p.setProperty("indexcompression", ""+indexcompression);
p.setProperty("indexmaxfilesize", ""+indexmaxfilesize);
p.setProperty("indexcharset", indexcharset);
BotUtil.storeProperties(p, f);
FileBot.getFileBot().loadSearchIndexes();

return "OK";