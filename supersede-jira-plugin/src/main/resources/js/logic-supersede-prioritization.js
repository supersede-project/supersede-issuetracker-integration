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
}