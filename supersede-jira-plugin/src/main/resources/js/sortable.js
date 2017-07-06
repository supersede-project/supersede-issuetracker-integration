var app = angular.module('orderReqs', []);
app
		.controller(
				'reqsCtrl',
				function($scope) {
					$scope.requirements = [
							{
								id : 'R0',
								title : 'First Requirement',
								description : 'description of the first requirement',
								characteristics : 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.',
								link : 'http://mantis/R0'
							},
							{
								id : 'R1',
								title : 'Second Requirement',
								description : 'description of the second requirement',
								characteristics : 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.',
								link : 'http://mantis/R1'
							},
							{
								id : 'R2',
								title : 'Third Requirement',
								description : 'description of the third requirement',
								characteristics : 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.',
								link : 'http://mantis/R2'
							},
							{
								id : 'R3',
								title : 'Fourth Requirement',
								description : 'description of the fourth requirement',
								characteristics : 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.',
								link : 'http://mantis/R3'
							},
							{
								id : 'R4',
								title : 'Fifth Requirement',
								description : 'description of the fifth requirement',
								characteristics : 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.',
								link : 'http://mantis/R4'
							} ];
				});
AJS.$(document).ready(function() {
	AJS.$("#sortable").jqxSortable();
	AJS.$(".jqxexpander").jqxExpander({
		theme : "summer",
		expanded : false,
		width : 200
	});
});