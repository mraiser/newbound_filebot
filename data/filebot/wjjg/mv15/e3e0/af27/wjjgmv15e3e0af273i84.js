var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
	    $('#fileupload').fileupload({
	        url: url,
	        dataType: 'json',
	        done: function (e, data) {
	        	$('#cblock').css('display', 'none');
	            spinner.stop();
	        	
	        	$('#files').html('File uploaded successfully.<br><br>');
	            setTimeout('loadFolder(curpath);', 1000);
	            setTimeout('loadFile(curpath+"/'+data.result.files[0].name+'");', 1500);
	        },
	        progressall: function (e, data) {
	            var progress = parseInt(data.loaded / data.total * 100, 10);
	            $('#progress .progress-bar').css(
	                'width',
	                progress + '%'
	            );
	        }
	    }).prop('disabled', !$.support.fileInput)
	        .parent().addClass($.support.fileInput ? undefined : 'disabled');
	});
  
  
};