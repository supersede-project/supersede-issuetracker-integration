package eu.supersede.jira.plugins.activeobject;

import net.java.ao.Entity;

public interface ReplanJiraLogin extends Entity {

	String getReplanUsername();

	void setReplanUsername(String username);

	String getJiraUsername();

	void setJiraUsername(String username);

}
