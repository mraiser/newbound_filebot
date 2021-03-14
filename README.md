# newbound_filebot

A collection of useful Newbound Metabot controls for searching and sharing files over the Newbound Network.

# Dependencies
This project requires an up-to-date working installation of the Newbound software
https://github.com/mraiser/newbound

# Installation
1. Move the data/filebot and runtime/filebot folders into your Newbound installation's data and runtime folders, respectively
2. Launch the Newbound software
3. Publish the "filebot" control in the "filebot" library using the Metabot app
4. Restart the Newbound software

*Instead of moving the data/filebot and runtime/filebot folders you can create symbolic links to them, leaving your git project folder intact for easy updating*

## File Search
To enable search, add the following property to runtime/filebot/botd.properties:
    
    searchindex=true

To index the content as well as filenames, add:

    indexcontent=true

*NOTE:* Changing botd.properties requires a restart of the Newbound software. The index is not built automatically, you must manually call the index endpoint. For example if you have shared your ~/Documents folder with filebot, enter the following into a web browser on that machine:

    http://localhost:5773/filebot/index?path=/Documents