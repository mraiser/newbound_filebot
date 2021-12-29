var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  debugger;
  var path = ME.DATA.path;
  if (!path || path == '') path = '/';
  
  if (path == '/'){
  }
  else {
    json('../filebot/fileinfo', 'path='+encodeURIComponent(path)+'&children=false', function(result){
      debugger;
      $(ME).find('.filename').text(JSON.stringify(result));
    });
  }  
  
  
};
