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