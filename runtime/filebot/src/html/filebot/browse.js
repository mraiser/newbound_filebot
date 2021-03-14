function fileBrowser(thediv, cb){
	var newhtml = 'BROWSE';
	
	
	$('#'+thediv).html(newhtml);
	$('#'+thediv).trigger('create');
}