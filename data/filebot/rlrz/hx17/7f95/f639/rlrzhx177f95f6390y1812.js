var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  componentHandler.upgradeAllRegistered();
  
  me.prefix = ME.DATA.peer ? '../peerbot/remote/'+ME.DATA.peer+'/' : '../';
  document.searchFiles = searchFiles;
};

function searchFiles(){
  $(ME).find('.filesearchresults').empty();
  var query = $(ME).find('.filesearchquery').val();
  var path = ME.DATA.path ? ME.DATA.path : '/';
  var params = 'path='+encodeURIComponent(path)+'&query='+encodeURIComponent(query)+'&uuid='+encodeURIComponent(me.UUID);
      
  json(me.prefix+'filebot/search', params, function(result){
    var el = $(ME).find('.filesearchresults');
    if (!me.gotwsdata){
      if (result.list.length>0)
        for (var i in result.list)
          addResultRow(result.list[i]);
      else el.append('<i>No results found. </i>');
    }
    me.gotwsdata = false;
    el.append('<i>Search done</i>');
  }, ME.DATA.peer);
}

function addResultRow(filename){
  var el = $('<a class="ablock" target="_blank"/>');
  el.prop('href', '../filebot/local'+filename);
  el.text(filename);
  $(ME).find('.filesearchresults').append(el);
}

me.wscb = function(x){
  me.gotwsdata = true;
  addResultRow(x.msg);
};

$(document).click(function(event) {
   window.lastElementClicked = event.target;
});
