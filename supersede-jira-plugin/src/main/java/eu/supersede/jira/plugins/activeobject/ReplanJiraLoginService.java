package eu.supersede.jira.plugins.activeobject;

import java.util.List;

import com.atlassian.activeobjects.tx.Transactional;

@Transactional
public interface ReplanJiraLoginService {

	public ReplanJiraLogin add(String replanUsername, String jiraUsername);
	
	public ReplanJiraLogin add(String replanUsername, String jiraUsername, String tenant);

	List<ReplanJiraLogin> getAllLogins();

	public ReplanJiraLogin getLoginByReplanUsername(String replanUsername, String tenant);

	public ReplanJiraLogin getLoginByJiraUsername(String jiraUsername, String tenant);

}
