AJS.$(document).ready(function() {
	var self = jQuery(this);
	jQuery.ajax({
		type : "get",
		url : "supersede-prioritization?loadIssues=y&filter=" + $('#filter-select').val(),
		success : function(data) {
			$("#issues-table-data").html(data);
		},
		error : function() {
			console.log('error', arguments);
		}
	});
	onPageLoad();
	// Needed just one time
});

function onPageLoad() {
	AJS.$(".icon-tooltip").tooltip();

	$('#filter-select').change(function() {
		$('#filter-select').val($(this).val());
		var self = jQuery(this);
		jQuery.ajax({
			type : "get",
			url : "supersede-prioritization?loadIssues=y&filter=" + $(this).val(),
			success : function(data) {
				console.log('dom', self, data);
				$("#issues-table-data").html(data);
			},
			error : function() {
				console.log('error', arguments);
			}
		});
		// alert("load!");
	});
	
	$('#create-features').click(function() {
		$('.procFilter').val($('#filter-select').val());
	});
}