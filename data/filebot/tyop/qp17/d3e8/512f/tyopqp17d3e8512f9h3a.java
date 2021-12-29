File root = BotBase.getBot("filebot").getRootDir();
File f = new File(root, "excludefromsearch.txt");
FileWriter fw = new FileWriter(f);
int i = list.length();
while (i-->0) fw.write(list.get(i)+"\n");
fw.close();
return "OK";