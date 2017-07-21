AJS.$(document).ready(
		function() {
			var self = jQuery(this);
			jQuery.ajax({
				type : "get",
				url : "supersede-prioritization?loadIssues=y&filter="
						+ $('#filter-select').val(),
				success : function(data) {
					console.log('dom', self, data);
					$("#export-data").html(data);
					// self.parent().parent().remove();
					onPageLoad();
				},
				error : function() {
					console.log('error', arguments);
				}
			});
			onPageLoad();
			$(".projectElement").first().addClass('aui-nav-selected')
					.removeClass('aui-nav');

		});

function onPageLoad() {

	$('#filter-select').change(
			function() {
				$('#filter-select').val($(this).val());
				var self = jQuery(this);
				jQuery.ajax({
					type : "get",
					url : "supersede-prioritization?loadIssues=y&filter="
							+ $(this).val(),
					success : function(data) {
						console.log('dom', self, data);
						$("#export-data").html(data);
						// self.parent().parent().remove();
						onPageLoad();
					},
					error : function() {
						console.log('error', arguments);
					}
				});
				// alert("load!");
			});
	
	AJS.$("#create-process-button").click(function() {
		AJS.dialog2("#create-process-dialog").show();
		$('.procFilter').val($('#filter-select').val());
	});
	
	$('#dialog-submit-button').click( function(){
		$('.procId').val($('#processId').val());
		$('.procDesc').val($('#description').val());
		$('.procFilter').val($('#filter-select').val());
	});
	
	$('#processId').ready(function() {
		$('#processId').val(AJS.I18n.getText("supersede-prioritization-create-process-default-id.label")+ new Date().getTime());
	});
	
	$("#show-processes-button").click(
	function() {
		var self = jQuery(this);
		jQuery.ajax({
			type : "get",
			url : "supersede-prioritization?showProcesses=y",
			success : function(data) {
				console.log('dom', self, data);
				$("#export-data").html(data);
				// self.parent().parent().remove();
				//onPageLoad();
			},
			error : function() {
				console.log('error', arguments);
			}
		});
		// alert("load!");
	});

//	$("#create-process-button").click(
//			function() {
//				alert($('#filter-select').val());
//				var self = jQuery(this);
//				jQuery.ajax({
//					type : "post",
//					url : "supersede-prioritization?createProcess=y&filter="
//							+ $('#filter-select').val(),
//					success : function(data) {
//						console.log('dom', self, data);
//						$("#export-data").html(data);
//						// self.parent().parent().remove();
//						//onPageLoad();
//					},
//					error : function() {
//						console.log('error', arguments);
//					}
//				});
//				// alert("load!");
//			});
}
