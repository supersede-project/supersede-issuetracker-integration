package eu.supersede.jira.plugins.activeobject;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;

public interface SupersedeProcess extends Entity {

	// Used as Project Name on Supersede
	String getProcId();

	void setProcId(String id);

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
	@StringLength(value=StringLength.UNLIMITED)
	String getIssuesRequirementsMap();

	//
	@StringLength(value=StringLength.UNLIMITED)
	void setIssuesRequirementsMap(String issuesRequirementsMap);

	// Creation date (since more than a process could be started with the same
	// query)

	Date getLastRankingImportDate();

	void setLastRankingImportDate(Date lastRankingImportDate);

	
	
	int getRankings();

	void setRankings(int rankings);

}
