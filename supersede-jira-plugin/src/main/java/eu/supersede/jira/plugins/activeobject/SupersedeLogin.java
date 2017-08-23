package eu.supersede.jira.plugins.activeobject;

import net.java.ao.Entity;

public interface SupersedeLogin extends Entity {
	String getJiraUser();

	void setJiraUser(String jiraUser);

	String getSSUser();

	void setSSUser(String ssUser);

	String getSSPassword();

	void setSSPassword(String ssPassword);
	
	String getTenant();

	void setTenant(String tenant);

}
