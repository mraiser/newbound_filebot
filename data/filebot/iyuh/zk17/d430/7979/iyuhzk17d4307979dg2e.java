File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "botd.properties");
Properties p = BotUtil.loadProperties(f);
File workdir;
if (p.getProperty("indexworkdir") != null) workdir = new File(p.getProperty("indexworkdir"));
else workdir = new File(root, ".index");
BotUtil.deleteDir(workdir);
workdir.mkdirs();

return "ok";