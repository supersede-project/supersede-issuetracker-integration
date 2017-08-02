package eu.supersede.jira.plugins.activeobject;

import java.util.HashMap;
import java.util.List;

import com.atlassian.activeobjects.tx.Transactional;


@Transactional
public interface ProcessService {
	
	SupersedeProcess add(String desc, String query);
	
	SupersedeProcess add(String desc, String query, String status);
	
	SupersedeProcess add(String desc, String processID, String issueRequirementsMap, String query, String status);
	
	List<SupersedeProcess> getAllProcesses();
	
	SupersedeProcess getProcess(String processId);
	
	void updateAllProcessesStatus(List<SupersedeProcess> processList);
	
	void addSingleIssue(int id, String issue);

}
