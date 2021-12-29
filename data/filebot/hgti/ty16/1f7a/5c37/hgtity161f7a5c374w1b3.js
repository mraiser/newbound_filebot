var filetypes = [ 'aac', 'ai', 'aiff', 'avi', 'bmp', 'c', 'cpp', 'css', 'dat', 'dmg', 'doc', 'dotx', 'dwg', 'dxf', 'eps', 'exe', 'flv', 'gif', 'h', 'hpp', 'html', 'ics', 'iso', 'java', 'jpg', 'js', 'key', 'less', 'mid', 'mp3', 'mp4', 'mpg', 'odf', 'ods', 'odt', 'otp', 'ots', 'ott', 'pdf', 'php', 'png', 'ppt', 'psd', 'py', 'qt', 'rar', 'rb', 'rtf', 'sass', 'scss', 'sql', 'tga', 'tgz', 'tiff', 'txt', 'wav', 'xls', 'xlsx', 'xml', 'yml', 'zip' ];

var me = this;
var ME = $('#'+me.UUID)[0];

function mf(sel){ return $(ME).find(sel); }

me.ready = function(){
  console.log(ME.DATA);

  var xx = ME.DATA;
  var path = xx.path;
  if (!path) {
    path = xx.path = '/remote/'+xx.id;
    xx.directory = true;
    mf('.demo-card-image__filename').text(xx.name);
  }
  else mf('.demo-card-image__filename').text(path.substring(path.lastIndexOf('/')+1));
  
  if (!xx.name) xx.directory = true;
  
  var img;
  if (xx.directory) {
    img = 'folder.png';
  }
  else {
      mf('.deletefile').css('display', 'block');
      var yy = xx.name.lastIndexOf(".");
      if (yy != -1) {
          var zz = filetypes.indexOf(xx.name.substring(yy+1).toLowerCase());
          if (zz != -1) img = 'icons/512px/'+filetypes[zz]+".png";
      }		
    
      mf('.deletefile').click(function(){
        installControl(mf('.popupdiv')[0], 'metabot', 'confirmdialog', function(){}, {
          title: 'Are you sure you want to delete this file? This cannot be undone.',
          cb: function(val){
            if (ME.DATA.peer){
              json('../peerbot/remote/'+ME.DATA.peer+'/filebot/deletefile','path='+encodeURIComponent(ME.DATA.path)+'&peer=local', function(){
                showFolder('/remote/'+ME.DATA.peer+ME.DATA.path.substring(0,ME.DATA.path.lastIndexOf('/')));
              });
            }
            else{
              json('../filebot/deletefile','path='+encodeURIComponent(ME.DATA.path)+'&peer=local', function(){
                showFolder('/local'+ME.DATA.path.substring(0,ME.DATA.path.lastIndexOf('/')));
              });
            }
          }
        });
      });
    
      if (xx.peer) mf('.clickme').css('display', 'block').prop('href', '../peerbot/remote/'+xx.peer+'/filebot/local'+ME.DATA.path);
      else mf('.clickme').css('display', 'block').prop('href', '../filebot/local'+ME.DATA.path);
    
//      mf('.clickme
  }
  if (img == null) img = 'icons/512px/_blank.png';
  img = '../filebot/img/'+img;
  
  mf('.demo-card-image.mdl-card').css('background-image', "url('"+img+"')");
};
