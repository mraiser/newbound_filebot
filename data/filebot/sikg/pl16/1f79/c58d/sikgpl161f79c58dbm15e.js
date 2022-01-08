var me = this;
var ME = $('#'+me.UUID)[0];

function mf(sel){ return $(ME).find(sel); }

me.ready = function(){
  var path = ME.DATA.path;
  if (!path || path == '') path = '/';
  else if (path == '/local') path = '/local/';
  else if (path == '/remote') path = '/remote/';

  if (path == '/'){
    mf('.folderbuttons').css('display', 'none');
    var el = $('<div class="fileinfowrap" />');
    el.click(function(){ showFolder('/local'); });
    mf('.folderinfo').append(el);
    var data = { path:"/local" };
    installControl(el[0], 'filebot', 'filecard', function(){}, data);
    
    el = $('<div class="fileinfowrap" />');
    el.click(function(){ showFolder('/remote'); });
    mf('.folderinfo').append(el);
    data = { path:"/remote" };
    installControl(el[0], 'filebot', 'filecard', function(){}, data);
  }
  else if (path.indexOf('/remote/') == 0) {
    if (path == '/remote/') {
      mf('.folderbuttons').css('display', 'none');
      $(ME).find('.searchbar').empty();
      json('../peerbot/connections', null, function(result){
        for (var i in result.data){
          var p = result.data[i];
          if (p.connected){
            function showRemote(data){
              var el = $('<div class="fileinfowrap" />');
              el.click(function(){ 
                showFolder('/remote/'+data.id, data.name); 
              });
              mf('.folderinfo').append(el);
              installControl(el[0], 'filebot', 'filecard', function(){}, data);
            }
            showRemote(p);
          }
        }
      });
    }
    else {
      path = path.substring(8);
      var i = path.indexOf('/');
      var peer = i == -1 ? path : path.substring(0,i);
      ME.DATA.peer = peer;
      path = i == -1 ? '/' : path.substring(i);
      if (i == -1) mf('.folderbuttons').css('display', 'none');
      else {
        mf('.downloadbutton').prop('href', '../peerbot/remote/'+peer+'/filebot/zipdir/archive.zip?path='+encodeURIComponent(path));
      
        var el = $(ME).find('.searchbar');
        var d = {
          "path": path,
          "peer": peer
        };
        installControl(el[0], 'filebot', 'search', function(api){}, d);
      }
      
      json('../peerbot/remote/'+peer+'/filebot/fileinfo', 'path='+encodeURIComponent(path)+'&children=false', function(result){
        if (result.list.length == 0) mf('.folderinfo').html('<div style="width:100000px;padding:40px;"><i>This folder is empty.</i></div>');
        else for (var i in result.list) {
          function showRemote(data){
              var el = $('<div class="fileinfowrap" />');
              el.click(function(){ 
                if (data.directory) showFolder('/remote/'+peer+data.path); 
      //          else showFile('/local'+data.path); 
              });
              mf('.folderinfo').append(el);
              data.peer = peer;
              installControl(el[0], 'filebot', 'filecard', function(){}, data);
          }
          
          var f = result.list[i];
          if (typeof f == 'object') showRemote(f);
          else {
            var newpath = path + (path.endsWith('/') ? f : '/'+f);
            json('../peerbot/remote/'+peer+'/filebot/fileinfo', 'path='+encodeURIComponent(newpath)+'&children=false', function(data){
              showRemote(data);
            });
          }
          
        }
      });
    }
    
  }
  else if (path.indexOf('/local/') == 0) {
    var el = $(ME).find('.searchbar');
    if (path == '/local/') {
      mf('.folderbuttons').css('display', 'none');
      el.empty();
    }
    else {
      mf('.folderbuttons').css('display', 'inline-block');
      var d = {
        "path": path.substring(6)
      };
      installControl(el[0], 'filebot', 'search', function(api){}, d);
    }
    
    mf('.downloadbutton').prop('href', '../filebot/zipdir/archive.zip?path='+encodeURIComponent(ME.DATA.path.substring(6)));

    function showLocal(data){
        var el = $('<div class="fileinfowrap" />');
        el.click(function(){ 
          if (data.directory) showFolder('/local'+data.path); 
//          else showFile('/local'+data.path); 
        });
        mf('.folderinfo').append(el);
        installControl(el[0], 'filebot', 'filecard', function(){}, data);
    }
    
    json('../filebot/fileinfo', 'path='+encodeURIComponent(path.substring(6))+'&children=false', function(result){
      if (result.list.length == 0) mf('.folderinfo').html('<div style="width:100000px;padding:40px;"><i>This folder is empty.</i></div>');
      else for (var i in result.list) {
        var f = result.list[i];
        if (typeof f == 'object') showLocal(f);
        else {
          var newpath = path.substring(6) + (path.endsWith('/') ? f : '/'+f);
          json('../filebot/fileinfo', 'path='+encodeURIComponent(newpath)+'&children=false', function(data){
            showLocal(data);
          });
        }
      }
    });
  }
  else {
    json('../filebot/fileinfo', 'path='+encodeURIComponent(path)+'&children=false', function(result){
      $(ME).find('.folderinfo').text(JSON.stringify(result));
    });
  }  
};

$(ME).find('.deletebutton').click(function(){
  installControl(mf('.popupdiv')[0], 'metabot', 'confirmdialog', function(){}, {
    title: 'Delete Folder',
    text: 'Are you sure you want to delete this folder and all of its contents? This cannot be undone.',
    cb: function(name){
      if (ME.DATA.peer) {
        var path = ME.DATA.path.substring(8+ME.DATA.peer.length);
        
		json('../peerbot/remote/'+ME.DATA.peer+'/filebot/deletefolder','path='+encodeURIComponent(path)+'&name='+encodeURIComponent(name)+'&peer=local', function(){
			showFolder(ME.DATA.path.substring(0,ME.DATA.path.lastIndexOf('/')));
		});
      }
      else {
		json('../filebot/deletefolder','path='+encodeURIComponent(ME.DATA.path.substring(6))+'&peer=local', function(){
			showFolder(ME.DATA.path.substring(0,ME.DATA.path.lastIndexOf('/')));
		});
      }
    }
  });
});

$(ME).find('.newbutton').click(function(){
  installControl(mf('.popupdiv')[0], 'metabot', 'promptdialog', function(){}, {
    title: 'New Folder',
    text: 'Folder Name',
    cb: function(name){

      if (ME.DATA.peer) {
        var path = ME.DATA.path.substring(8+ME.DATA.peer.length);
        
		json('../peerbot/remote/'+ME.DATA.peer+'/filebot/newfolder','path='+encodeURIComponent(path)+'&name='+encodeURIComponent(name)+'&peer=local', function(){
	        showFolder(ME.DATA.path);
		});
      }
      else {
		json('../filebot/newfolder','path='+encodeURIComponent(ME.DATA.path.substring(6))+'&name='+encodeURIComponent(name)+'&peer=local', function(){
	        showFolder(ME.DATA.path);
		});
      }
      
    }
  });
});


$(ME).find('.uploadbutton').click(function(){
  var path = ME.DATA.peer ? ME.DATA.path.substring(8+ME.DATA.peer.length) : ME.DATA.path.substring(6);  
  installControl(mf('.popupdiv')[0], 'filebot', 'uploaddialog', function(){}, {
    peer: ME.DATA.peer,
    path: path,
    cb: function(val){
      showFolder(ME.DATA.path);
    }
  });
});