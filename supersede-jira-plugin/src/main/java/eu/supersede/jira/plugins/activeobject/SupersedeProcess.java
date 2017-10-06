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

package eu.supersede.jira.plugins.activeobject;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;

public interface SupersedeProcess extends Entity {

	// Used as Project Name on Supersede
	String getProcId();

	void setProcId(String id);

	String getName();

	void setName(String name);

	// Description
	String getDescription();

	void setDescription(String desc);

	// In progress, Done, Saved
	String getStatus();

	void setStatus(String status);

	// Selection query of the process
	String getQuery();

	void setQuery(String query);

	// Issue List (populated on creation)
	String getIssues();

	void setIssues(String issues);

	// SS Project Id (needed in order to get all data regarding this project on
	// SS)
	String getSSProjectId();

	void setSSProjectId(String id);

	//
	// // Mapping betweeen Issues and Requirements, because 2 Processes could be
	// // going involving the same issue
	@StringLength(value = StringLength.UNLIMITED)
	String getIssuesRequirementsMap();

	//
	@StringLength(value = StringLength.UNLIMITED)
	void setIssuesRequirementsMap(String issuesRequirementsMap);

	// Creation date (since more than a process could be started with the same
	// query)

	Date getLastRankingImportDate();

	void setLastRankingImportDate(Date lastRankingImportDate);

	int getRankings();

	void setRankings(int rankings);

}
