package eu.supersede.jira.plugins.activeobject;

import java.util.List;

import com.atlassian.activeobjects.tx.Transactional;

@Transactional
public interface ReplanJiraLoginService {

	public ReplanJiraLogin add(String replanUsername, String jiraUsername);

	List<ReplanJiraLogin> getAllLogins();

	public ReplanJiraLogin getLoginByReplanUsername(String replanUsername);

	public ReplanJiraLogin getLoginByJiraUsername(String jiraUsername);

}
