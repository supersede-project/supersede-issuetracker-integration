package eu.supersede.jira.plugins.activeobject;

import net.java.ao.Entity;

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

	void setSSProjectId(int id);

//	// Mapping betweeen Issues and Requirements, because 2 Processes could be
//	// going involving the same issue
//	HashMap<String, String> getIssueRequirementsMap();
//
//	void setIssuesRequirementsMap(HashMap<String, String> issuesRequirementsMap);
//
//	// Creation date (since more than a process could be started with the same
//	// query)
//
//	Date getCreationDate();
//
//	void setCreationDate(Date creationDate);

}
