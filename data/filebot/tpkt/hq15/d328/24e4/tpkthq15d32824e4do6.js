var me = this;
var ME = $('#'+me.UUID)[0];

$(ME).find('.menubutton').click(function(){
  window.location.href='../';
});

me.ready = function(){
  showPath('/');
  
};


$(ME).find('.settingsbutton').click(function(){
  window.location.href='../filebot/settings.html';
});

function showPath(path, altname){
  var data = { path:path };
  installControl($(ME).find('.folderinfowrap')[0], 'filebot', 'folderinfo', function(){}, data);

  var name = path == '/' ? 'filebot' : path.substring(path.lastIndexOf('/')+1);
  var path2 = 'filebot_'+path.hashCode();

  var chip = $(ME).find('.filebot'+path2);
  if (chip[0]) while (chip.next()[0]) chip.next().remove();
  else {
    if (altname) name = altname;
    var el = $('<span class="mdl-chip filebot'+path2+'"><span class="mdl-chip__text">'+name+'</span></span>');
    el.click(function(){ showPath(path, el); });
    $(ME).find('.chips').append(el);
  }
}

showFolder = showPath;

String.prototype.hashCode = function() {
  var hash = 0, i, chr;
  if (this.length === 0) return hash;
  for (i = 0; i < this.length; i++) {
    chr   = this.charCodeAt(i);
    hash  = ((hash << 5) - hash) + chr;
    hash |= 0; // Convert to 32bit integer
  }
  return hash;
};