var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  componentHandler.upgradeAllRegistered();
  send_statistics(function(result){
    console.log(result);
  });
};

$(document).click(function(event) {
   window.lastElementClicked = event.target;
});
