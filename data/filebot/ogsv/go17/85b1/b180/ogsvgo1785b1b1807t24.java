String DIRNAME = "8cee109e-8684-43a1-ada5-eca55e4ba55d";
String ROOT = "/root/Newbound/runtime/filebot/.index/x-149979031/Projects/old/";

File dir = new File(ROOT);

JSONObject jo = new JSONObject();

while (true)
{
  String[] subdirs = dir.list();
  int max = 0;
  File fmax = null;
  for (int i=0; i<subdirs.length; i++)
  {
    if (!subdirs[i].equals(DIRNAME))
    {
      File w = new File(dir, subdirs[i]);
      w = new File(w, DIRNAME);
      boolean isdir = w.exists();
      if (isdir || w.getParentFile().exists())
      {
        BitSet bs = BitSet.valueOf(BotUtil.readFile(isdir ? w : w.getParentFile()));
        //BitSet bs = BitSet.valueOf(BotUtil.readFile(new File("/root/Newbound/runtime/filebot/.index/x-149979031/Projects/old/workspace_20091026/L")));
        int count = 0;
        int n = bs.size();
        while (n-->0) if (bs.get(n)) count++;
        if (count>max)
        {
          max = count;
          fmax = w;
        }
        
        //break;
      }
    }
  }
  if (fmax == null) break;
  
  boolean isdir = fmax.exists();
  fmax = fmax.getParentFile();
  jo.put(fmax.getCanonicalPath(), max);
  
  if (!isdir) break;
  
  dir = fmax;
}

return jo;