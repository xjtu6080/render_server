// JavaScript Document

$(document).ready(function() {

	var pageNum = parseInt($('[name=pageNum]').val());
	var pageSize = 10;
	var pageTotal = parseInt($('[name=pageTotal]').val());
	var searchText = $('[name=searchText]').val();
	var searchType = $('[name=searchType]').val();
	
	var pageHtml = '';
	pageSize = 10;
	if(pageNum > 1){
		pageHtml += "<a href=\"#\" class=\"lastPage\">上一页</a>";
	}
	if(pageTotal<=pageSize){
		for(var i=1; i<=pageTotal; i++){
			if(i == pageNum){
				pageHtml += "<span class=\"current\">" + i + "</span>";
			}else{
				pageHtml += "<a href=\"#\" class=\"curved\">" + i + "</a>";
			}
		}
	}else{
		if((pageNum - pageSize/2) <= 0){
			for(var i=1; i<=(pageTotal>pageSize?pageSize:pageTotal); i++){
				if(i == pageNum){
					pageHtml += "<span class=\"current\">" + i + "</span>";
				}else{
					pageHtml += "<a href=\"#\" class=\"curved\">" + i + "</a>";
				}
			}
		}else if((pageNum - pageSize/2) > 0 && (pageTotal - pageNum) >= pageSize/2){
			for(var i=(pageNum - pageSize/2 + 1); i<=(pageNum + 5); i++){
				if(i == pageNum){
					pageHtml += "<span class=\"current\">" + i + "</span>";
				}else{
					pageHtml += "<a href=\"#\" class=\"curved\">" + i + "</a>";
				}
			}
		}else if((pageNum - pageSize/2) > 0 && (pageTotal - pageNum) < pageSize/2){
			for(var i=(pageTotal-pageSize + 1); i<=pageTotal; i++){
				if(i == pageNum){
					pageHtml += "<span class=\"current\">" + i + "</span>";
				}else{
					pageHtml += "<a href=\"#\" class=\"curved\">" + i + "</a>";
				}
			}
		}
	}
	if(pageNum < pageTotal){
		pageHtml += "<a href=\"#\" class=\"nextPage\">下一页</a>";
	}
	
	$('.pagination').empty();
	$('.pagination').append(pageHtml);
	
	$('.curved').click(function(){
		$('[name=pageNum]').val($(this).html());
		$('[name=searchType]').val(searchType);
		$('[name=searchText]').val(searchText);
		$('#user-search-form').submit();	
	});
	$('.nextPage').click(function(){
		$('[name=pageNum]').val(pageNum+1);
		$('[name=searchType]').val(searchType);
		$('[name=searchText]').val(searchText);
		$('#user-search-form').submit();
	});
	$('.lastPage').click(function(){
		$('[name=pageNum]').val(pageNum-1);
		$('[name=searchType]').val(searchType);
		$('[name=searchText]').val(searchText);
		$('#user-search-form').submit();
	});
	
	if(searchType == 'name'){
		$('#user-search-name').attr('selected', 'selected');
	}else if(searchType == 'username'){
		$('#user-search-username').attr('selected', 'selected');
	}
		
	$('#searchBtn').click(function(){
		$('[name=searchText]').val($('#user-search-text').val());
		$('[name=pageNum]').val(1);
		if($('#user-search-name').attr('selected') == 'selected'){
			$('[name=searchType]').val('name');
		}else if($('#user-search-username').attr('selected') == 'selected'){
			$('[name=searchType]').val('username');
		}
		$('#user-search-form').submit();
	});
	
    $('.userTable :checkbox.toggle').each(function(i, toggle) {
        $(toggle).change(function(e) {
          $(toggle).parents('table:first').find(':checkbox:not(.toggle)').each(function(j, checkbox) {
            checkbox.checked = !checkbox.checked;
          });
        });
      });
    
	$('#deleteAllBtn').click(function(){
		if($(":checkbox[name=userIds]:checked").size()==0){
			alert('请选择您要删除的记录!'); 
			return false;
		}
		$('#user-delete-form').submit();		
	});
    
});