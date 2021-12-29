if (path.startsWith("/local/"))
{
  File src = new File(BotBase.getBot("botmanager").getRootDir(), "data");
  src = new File(src, "filebot_runtime");
  src = new File(src, "_ASSETS");
  src = new File(src, name);
  
  InputStream is = new FileInputStream(src);

  File dst = new File(path.substring(6));
  dst = new File(dst, name);
  
  ((FileBot)BotBase.getBot("filebot")).setFileContent(dst.getCanonicalPath(), is, (int)src.length());
  is.close();
  src.delete();
}
return "OK";