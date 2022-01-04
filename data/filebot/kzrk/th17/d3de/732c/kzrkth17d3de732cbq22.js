var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  componentHandler.upgradeAllRegistered();
  
  send_getindexsettings(function(result){
    console.log(result);
    if (result.data.searchindex){
      var el = $(ME).find('.searchindex');
      el.find('input').prop('checked', true);
      el[0].MaterialSwitch.checkToggleState();
      $(ME).find('.restofthesettings').css('display', 'block');
      
      if (result.data.indexcontent){
        el = $(ME).find('.indexcontent');
        el.find('input').prop('checked', true);
        el[0].MaterialSwitch.checkToggleState();
        
        el = $(ME).find('.indexworkdir');
        el.find('input').val(result.data.indexworkdir);
        el[0].MaterialTextfield.checkDirty();
        
        el = $(ME).find('.indexcompression');
        el.val(result.data.indexcompression);
        
        el = $(ME).find('.indexmaxfilesize');
        el.val(result.data.indexmaxfilesize);

        el = $(ME).find('.indexcharset');
        el.find('input').val(result.data.indexcharset);
        el[0].MaterialTextfield.checkDirty();
      }
    }
    
    json('../filebot/fileinfo', 'path=/&children=true', function(result){
      me.shares = result.list;
      buildShares();
      send_getexcludes(function(result){
        me.excludes = result.data.list;
        buildExcludes();
      });
    });

  });
};
/*
$(ME).find('.testindex').click(function(){
  send_testindexsettings(function(result){
    console.log(result);
  });
});
*/
$(ME).find('.saveindexsettings').click(function(){
  var searchindex = $(ME).find('.searchindex').find('input').prop('checked');
  var indexcontent = $(ME).find('.indexcontent').find('input').prop('checked');
  var indexworkdir = $(ME).find('.indexworkdir').find('input').val();
  var indexcompression = $(ME).find('.indexcompression').val();
  var indexmaxfilesize = $(ME).find('.indexmaxfilesize').val();
  var indexcharset = $(ME).find('.indexcharset').find('input').val();
  send_saveindexsettings(searchindex, indexcontent, indexworkdir, indexcompression, indexmaxfilesize, indexcharset, function(result){
    if (result.status != 'ok') alert(result.msg);
    else{
      var settings = '';
      $(ME).find('.sharetable').find('tr').each(function(x,y){
        var vals = $(y).find('input');
        if (vals[1]){
          var name = vals[0].value;
          var path = vals[1].value;
          if (name != ''){
            if (settings != '') settings += '\r';
            settings += name+'\t'+path;
          }
        }
      });
      var list = [];
      $(ME).find('.excludetable').find('tr').each(function(x,y){
        var vals = $(y).find('input');
        if (vals[0]){
          var path = vals[0].value;
          if (path != '') list.push(path);
        }
      });
      send_setexcludes(list, function(result){
        if (result.status != 'ok') alert(result.msg);
        else {
          json("../filebot/updatefileshare","v="+encodeURIComponent(settings), function(result){
            console.log(result);
            if (result.status != 'ok') alert(result.msg);
            else {
              document.location.href = '../filebot/index.html';
            }
          });
        }
      });
    }
  });
});

$(ME).find('.addsharebutton').click(function(){
  me.shares.push({name:'',realpath:''});
  buildShares();
});

$(ME).find('.addexcludebutton').click(function(){
  me.excludes.push('');
  buildExcludes();
});

$(ME).find('.searchindex').find('input').change(function(){
  $(ME).find('.restofthesettings').css('display', $(this).prop('checked') ? 'block' : 'none');
});

function buildExcludes(){
  if (me.excludes.length == 0) $(ME).find('.addexcludebutton').click();
  var newhtml = '<table class="mdl-data-table mdl-js-data-table mdl-shadow--2dp"><thead><tr><th class="mdl-data-table__cell--non-numeric">Delete</th><th class="mdl-data-table__cell--non-numeric">Path</th></tr></thead><tbody class="addshareshere">';
  for (var item in me.excludes){
    var rli = me.excludes[item];
    newhtml += buildExclude(rli);
  }
  newhtml += '</tbody></table>';
  var el = $(ME).find('.excludetable');
  el.html(newhtml);
  el.find('.deletesetting').click(function(e){
    var rli = $(this).closest('tr').find('input')[0].value;
    me.excludes.splice(me.excludes.indexOf(rli), 1);
    buildExcludes();
  });;  
  componentHandler.upgradeAllRegistered();
}

function buildExclude(rli){
  var id1 = guid();
  return '<tr><td class="mdl-data-table__cell--non-numeric"><button class="mdl-button mdl-button--icon mdl-js-button mdl-js-ripple-effect deletesetting"><i class="material-icons">delete</i></button></td><td class="mdl-data-table__cell--non-numeric">'
    + '<form action="#"><div class="mdl-textfield mdl-js-textfield"><input class="mdl-textfield__input" type="text" id="'
    + id1
    + '" value="'
    + rli
    +'"><label class="mdl-textfield__label" for="'
    + id1
    + '">Path to exclude</label></div>'
    + '</form>'
    + '</td></tr>';
}

function buildShares(){
  if (me.shares.length == 0) $(ME).find('.addsharebutton').click();
  var newhtml = '<table class="mdl-data-table mdl-js-data-table mdl-shadow--2dp"><thead><tr><th class="mdl-data-table__cell--non-numeric">Delete</th><th class="mdl-data-table__cell--non-numeric">Index</th><th class="mdl-data-table__cell--non-numeric">Name</th><th class="mdl-data-table__cell--non-numeric">Path</th></tr></thead><tbody class="addshareshere">';
  for (var item in me.shares){
    var rli = me.shares[item];
    newhtml += buildShare(rli);
  }
  newhtml += '</tbody></table>';
  var el = $(ME).find('.sharetable');
  el.html(newhtml);
  el.find('.deletesetting').click(function(e){
    var path = $(this).closest('tr').find('input')[1].value;
    var rli = getByProperty(me.shares, 'realpath', path);
    me.shares.splice(me.shares.indexOf(rli), 1);
    buildShares();
  });;
  el.find('.refreshindex').click(function(e){
    var el = $(this).closest('tr');
    var name = el.find('input')[0].value;
    el.find('.lastindexdate').html('in progress');
    json('../filebot/index', 'path=/'+name, function(result){
      el.find('.lastindexdate').html(result.status == 'ok' ? 'just now' : "Error: "+result.msg);
    });
  });;
  componentHandler.upgradeAllRegistered();
}

function buildShare(rli){
  var id1 = rli.id1 = guid();
  var id2 = rli.id2 = guid();
  return '<tr><td class="mdl-data-table__cell--non-numeric"><button class="mdl-button mdl-button--icon mdl-js-button mdl-js-ripple-effect deletesetting"><i class="material-icons">delete</i></button></td><td class="mdl-data-table__cell--non-numeric"><button class="mdl-button mdl-button--icon mdl-js-button mdl-js-ripple-effect refreshindex"><i class="material-icons">refresh</i></button></td><td class="mdl-data-table__cell--non-numeric">'
    + '<form action="#"><div class="mdl-textfield mdl-js-textfield sharename"><input class="mdl-textfield__input" type="text" id="'
    + id1
    + '" value="'
    + rli.name
    +'"><label class="mdl-textfield__label" for="'
    + id1
    + '">Share Name</label></div>'
    + '<div style="text-align:right;">Last index:</div></form>'
    + '</td><td class="mdl-data-table__cell--non-numeric">'
    + '<form action="#"><div class="mdl-textfield mdl-js-textfield"><input class="mdl-textfield__input" type="text" id="'
    + id2
    + '" value="'
    + rli.realpath
    +'"><label class="mdl-textfield__label" for="'
    + id2
    + '">Path to share</label></div>'
    + '<div><i class="lastindexdate">'+(rli.lastindex == -1 || typeof rli.lastindex == 'undefined' ? 'never' : parseDate(new Date(rli.lastindex)))+'</i></div></form>'
    + '</td></tr>';
}

$(ME).find('.deleteindex').click(function(){
  var data = {
    title:'Delete Index',
    text:'Are you sure you want to delete your search index? This will completely erase the contents of '+$(ME).find('.indexworkdir').find('input').val()+' on disk. This cannot be undone.',
    cancel:'cancel',
    ok:'delete',
    cb: function(){
      send_deleteindexes(function(result){
        alert(result.msg);
        setTimeout(buildShares, 3000);
      });
    }
  };
  installControl($(ME).find('.dialogdiv')[0], 'metabot', 'confirmdialog', function(){}, data);
});

$(ME).find('.refreshindex').click(function(){
  var list = [];
  for (var item in me.shares){
    var rli = me.shares[item];
    list.push(rli);
    var el = $('#'+rli.id2).closest('td');
    el.find('.lastindexdate').html('in progress');
  }
  function popNext(){
    if (list.length>0){
      var rli = list.pop();
      var name = rli.name;
      json('../filebot/index', 'path=/'+name, function(result){
        var el = $('#'+rli.id2).closest('td');
        el.find('.lastindexdate').html(result.status == 'ok' ? 'just now' : "Error: "+result.msg);
        popNext();
      });
    }
    else alert('INDEX DONE');
  }
  popNext();
});

$(document).click(function(event) {
   window.lastElementClicked = event.target;
});
