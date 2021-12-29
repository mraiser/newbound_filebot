JSONObject jo = new JSONObject();
JSONArray ja = new JSONArray();
jo.put("list", ja);
File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "excludefromsearch.txt");
if (f.exists()){
  String s = new String(BotUtil.readFile(f));
  String[] sa = s.split("\n");
  int i = sa.length;
  while (i-->0) if (!sa[i].equals("")) ja.put(sa[i]);
}
return jo;