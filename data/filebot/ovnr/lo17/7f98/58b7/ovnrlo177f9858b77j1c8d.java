boolean indexcontent = true;
boolean searchcontent = true;

DirectoryIndex di = new DirectoryIndex(
        new File("/home/mraiser/"),
        new File("/var/lib/newbound/directoryindex"),
        new NoDotFilter(),
        "abcdefghijklmnopqrstuvwxyz0123456789.-_",
        (short)3,
        1,
        indexcontent,
        50 * 1024 * 1024);

FileVisitor v = new SimpleFileVisitor()
{
    @Override
    public FileVisitResult visitFile(Object o, BasicFileAttributes basicFileAttributes) throws IOException
    {
        System.out.println(((File)o).getCanonicalPath());
        JSONObject jo = new JSONObject();
        jo.put("guid", uuid);
        jo.put("msg", ((File)o).getCanonicalPath());
        BotBase.getBot("metabot").sendWebsocketMessage(jo.toString());
        return null;
    }
};
di.search(query, v, searchcontent, false);

return "OK";