package eu.supersede.jira.plugins.logic;

import java.util.List;

import com.atlassian.activeobjects.tx.Transactional;

import eu.supersede.jira.plugins.servlet.SupersedeProcess;

@Transactional
public interface ProcessService {
	
	SupersedeProcess add(String id, String desc, String query);
	
	SupersedeProcess add(String id, String desc, String query, String status);
	
	List<SupersedeProcess> getAllProcesses();

}
