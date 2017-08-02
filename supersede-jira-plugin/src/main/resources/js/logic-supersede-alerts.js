AJS.$(document).ready(
		function() {
			$('#project-select-import').val($('.projectField').val()).trigger(
					'change');
			$('#project-select-attach').val($('.projectField').val()).trigger(
					'change');
			onPageLoad();
		});

function onPageLoad() {
	AJS.$('#project-select-import').auiSelect2();
	AJS.$('#project-select-attach').auiSelect2();
	$(".toEnable").prop('disabled', true);
	$(".toEnableDialog").prop('disabled', true);
	var selectionString = '';
	var issuesSelectionString = '';

	jQuery('.check-alerts').click(function() {
		var self = jQuery(this);
		jQuery.ajax({
			type : "get",
			url : "supersede-alerts?refreshAlerts=y",
			success : function(data) {
				console.log('dom', self, data);
				$("#data").html(data);
				// self.parent().parent().remove();
				AJS.tablessortable.setTableSortable(AJS.$(".sortableTable"));
				onPageLoad();
			},
			error : function() {
				console.log('error', arguments);
			}
		});
	});

	jQuery('.searchBtn').click(
			function() {
				$("#selectionList").val(selectionString);
				var self = jQuery(this);
				var searchStr = $('#searchAlertsInput').val();
				if (!searchStr) {
					searchStr = "";
				}
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?searchAlerts=y&searchAlertsInput="
							+ searchStr,
					success : function(data) {
						console.log('dom', self, data);
						$("#data").html(data);
						// self.parent().parent().remove();
						AJS.tablessortable.setTableSortable(AJS
								.$(".sortableTable"));
						onPageLoad();
					},
					error : function() {
						console.log('error', arguments);
					}
				});
			});

	jQuery('.searchBtnDialog').click(
			function() {
				$("#selectionList").val(selectionString);
				var self = jQuery(this);
				var searchStr = $('#searchAlertsInput').val();
				var searchIssueStr = $('#searchIssuesDialogInput').val();
				if (!searchStr) {
					searchStr = " ";
				}
				if (!searchIssueStr) {
					searchIssueStr = " ";
				}
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?searchIssues=y&searchAlertsInput="
							+ searchStr + "&searchIssuesInput="
							+ searchIssueStr,
					success : function(data) {
						console.log('dom', self, data);
						$("#attach-dialog-data").html(data);
						// self.parent().parent().remove();
						AJS.tablessortable.setTableSortable(AJS
								.$(".sortableDialogTable"));
						onPageLoad();
						// alert("load!");
					},
					error : function() {
						console.log('error', arguments);
					}
				});
			});

	jQuery("#searchAlertsInput").keyup(function(event) {
		if (event.keyCode == 13) {
			$(".searchBtn").click();
		}
	});

	jQuery('.chkSelected').click(
			function() {
				if ($(this).prop('checked')) {
					// Write a string containing the IDs of selected
					// alerts
					selectionString += $(this).attr('id');
					selectionString += ':::';
					$(".toEnable").prop('disabled', false);
					$(".toEnable").prop('enabled', true);
					// alert(selectionString);
				} else {
					// Remove the selected ID if checkbox gets unchecked
					selectionString = selectionString.replace($(this)
							.attr('id')
							+ ':::', '');
					if (!selectionString) {
						$(".toEnable").prop('disabled', true);
						$(".toEnable").prop('enabled', false);
					}
				}
			});
	jQuery('.dialogChkSelected').click(
			function() {
				alert("hi mom");
				if ($(this).prop('checked')) {
					// Write a string containing the IDs of selected
					// alerts
					// alert("entered");
					issuesSelectionString += $(this).attr('id');
					issuesSelectionString += ':::';
					$(".toEnableDialog").prop('disabled', false);
					$(".toEnableDialog").prop('enabled', true);
				} else {
					// Remove the selected ID if checkbox gets unchecked
					issuesSelectionString = issuesSelectionString.replace($(
							this).attr('id')
							+ ':::', '');
					if (!issuesSelectionString) {
						$(".toEnableDialog").prop('disabled', true);
						$(".toEnableDialog").prop('enabled', false);
					}
				}
			});

	jQuery('#chkDeleteOnClick').click(function() {
		// alert("triggereabbd")
		if ($(this).prop('checked')) {
			$(".chkDeleteStatus").val("true");
		} else {
			$(".chkDeleteStatus").val("true");
		}
		// alert($(this).prop('checked'));
	});

	var opt = {
		autoOpen : false,
		modal : true,
		width : 550,
		height : 650,
		title : 'Details'
	};

	jQuery('.opener').click(function() {
		$("#dialog-1").dialog(opt).dialog("open");
	});

	jQuery('.alertManagement').click(function() {
		console.log('alert management');
		$(".selectionList").val(selectionString);
	});

	// Shows the dialog when the "Show dialog" button is clicked
	AJS.$("#attach-dialog-show-button").click(function() {
		AJS.dialog2("#attach-dialog").show();
	});

	AJS.$("#import-dialog-show-button").click(function() {
		AJS.dialog2("#import-dialog").show();
	});

	AJS.$("#dialog-delete-button").click(function() {
		AJS.dialog2("#delete-dialog").show();
	});

	// Hides the dialog
	AJS.$("#dialog-close-button").click(function(e) {
		e.preventDefault();
		AJS.dialog2("#attach-dialog").hide();
	});

	// DIALOG
	jQuery('.dialogButton').click(function() {
		console.log('alert management')
		$("#issuesSelectionList").val(issuesSelectionString);
	});

	// DIALOG
	jQuery('.dialogDeleteButton').click(function() {
		console.log('alert management')
		$("#selectionList").val(selectionString);
	});

	jQuery('.projectElement').click(function() {
		$(".projectElement").removeClass('aui-nav-selected');
		$(".projectElement").addClass('aui-nav');
		$(this).removeClass('aui-nav').addClass('aui-nav-selected');
	});

	AJS.$(".simple-tooltip").tooltip();

	$('#project-select-import').change(function() {
		$('.projectField').val($(this).val());
	});

	$('#project-select-attach').change(
			function() {
				$('.projectField').val($(this).val());
				var self = jQuery(this);
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?searchIssues=y&projectField="
							+ $(this).val(),
					success : function(data) {
						console.log('dom', self, data);
						$("#attach-dialog-data").html(data);
						// self.parent().parent().remove();
						AJS.tablessortable.setTableSortable(AJS
								.$(".sortableDialogTable"));
					},
					error : function() {
						console.log('error', arguments);
					}
				});
				// alert("load!");
			});

	// MOVED TO THE .searchBtn function above
	// jQuery('.searchBtn').click(function() {
	// $("#selectionList").val(selectionString);
	// });

	// jQuery('.clickable-dialog').mousedown(function() {
	// $(".clickable-dialog").prop('href', '#'+$(this).prop('id'));
	// });
}