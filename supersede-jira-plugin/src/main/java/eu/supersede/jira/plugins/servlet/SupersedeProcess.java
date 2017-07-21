package eu.supersede.jira.plugins.servlet;

import java.util.List;

import net.java.ao.Entity;

public interface SupersedeProcess extends Entity {
	
	String getProcId();
	
	void setProcId(String id);
	
	String getDescription();
	
	void setDescription(String desc);
	
	String getStatus();
	
	void setStatus(String status);
	
	String getQuery();
	
	void setQuery(String query);

}
