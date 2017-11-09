/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

AJS.$(document).ready(function() {
	$('#page-title').text('Prioritization');
	var self = jQuery(this);
	jQuery.ajax({
		type : "get",
		url : "supersede-consts?loadIssues=y&filter=" + $('#filter-select').val(),
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
	AJS.$("#prioritization-help").tooltip();
	AJS.$(".rankingButton").tooltip();

	$('#filter-select').change(function() {
		$('#filter-select').val($(this).val());
		var self = jQuery(this);
		jQuery.ajax({
			type : "get",
			url : "supersede-consts?loadIssues=y&filter=" + $(this).val(),
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

	AJS.$("#create-process-button").click(function() {
		AJS.dialog2("#create-process-dialog").show();
		$('.procFilter').val($('#filter-select').val());
	});

	$('#dialog-submit-button').click(function() {
		$('.procId').val($('#processId').val());
		$('.procDesc').val($('#description').val());
		$('.procFilter').val($('#filter-select').val());
	});

	$('#processId').ready(function() {
		$('#processId').val(AJS.I18n.getText("supersede-prioritization-create-process-default-id.label") + new Date().getTime());
	});

	$("#process-start-button").click(function() {
		alert($(this).closest("tr").find(".issueQuery").text());
		$(this).closest("tr").find(".issueQuery").text();
	});

	$("#process-save-button").click(function() {
		alert("save");
	});

	$('.rankingButton').click(function() {
		$('#ranking-process-id').val($(this).attr('id'));
	});

	$(".collapsible-header").click(function() {
		$header = $(this);
		// getting the next element
		$content = $header.next();
		// open up the content needed - toggle the slide- if visible, slide up,
		// if not slidedown.
		$content.slideToggle(500, function() {
			// execute this after slideToggle is done
			// change text of header based on visibility of content div
			$header.text(function() {
				// change text based on condition
				if ($content.is(":visible")) {
					$('.collapsible-span').removeClass('glyphicon-chevron-down');
					$('.collapsible-span').addClass('glyphicon-chevron-up');
				} else {
					$('.collapsible-span').removeClass('glyphicon-chevron-up');
					$('.collapsible-span').addClass('glyphicon-chevron-down');
				}
			});
		});

	});

}
