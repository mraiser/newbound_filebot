var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  componentHandler.upgradeAllRegistered();
  me.peer = ME.DATA.peer;
  me.prefix = ME.DATA.peer ? '../peerbot/remote/'+ME.DATA.peer+'/' : '../';
  document.searchFiles = searchFiles;
};

function searchFiles(){
  $(ME).find('.filesearchresults').empty();
  
    var url = document.URL;
    url = url.substring(url.indexOf(':'));
    url = url.substring(0, url.indexOf('/',3));
    url = 'ws'+url+(typeof me.peer == 'string' ? '/peerbot/remote/'+me.peer+'/filebot/index.html' : '/filebot/index.html');

    var connection = new WebSocket(url, ['newbound']);

    connection.onopen = function(){
      console.log('Web Socket to filebot open: '+url);


  
      var query = $(ME).find('.filesearchquery').val();
      var path = ME.DATA.path ? ME.DATA.path : '/';
      var uuid = guid(); //me.UUID;
      var params = 'path='+encodeURIComponent(path)+'&query='+encodeURIComponent(query)+'&uuid='+encodeURIComponent(uuid);

      var now = new Date().getTime();
      json(me.prefix+'filebot/search', params, function(result){
        var el = $(ME).find('.filesearchresults');
        if (result.status == 'ok'){
          if (!me.gotwsdata){
            if (result.list.length>0)
              for (var i in result.list)
                addResultRow(result.list[i]);
            else el.append('<i>No results found. </i>');
          }
          me.gotwsdata = false;
          el.append('<i>Search done in '+(new Date().getTime()-now)+'ms</i>');
        }
        else el.append('<i>'+result.msg+'</i>');
        connection.close();
      }, ME.DATA.peer);
      
      
    };

    connection.onerror = function(error){
      console.log('Web Socket to filebot error');
      connection.close();
    };

    connection.onclose = function(error){
      console.log('Web Socket to filebot close');
    };

    connection.onmessage = function(e){
      var data = JSON.parse(e.data);
      console.log(data)
      me.wscb(data);
    };

}

function addResultRow(filename){
  var el = $('<a class="ablock" target="_blank"/>');
  el.prop('href', me.prefix+'filebot/local'+filename);
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
