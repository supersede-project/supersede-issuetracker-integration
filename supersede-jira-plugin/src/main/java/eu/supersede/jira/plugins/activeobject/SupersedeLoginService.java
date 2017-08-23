package eu.supersede.jira.plugins.activeobject;

import com.atlassian.activeobjects.tx.Transactional;

@Transactional
public interface SupersedeLoginService {

	SupersedeLogin add(String jiraUser, String ssUser, String ssPassword, String tenant);

	SupersedeLogin update(SupersedeLogin source, String jiraUser, String ssUser, String ssPassword, String tenant);

	SupersedeLogin getLogin(String jiraUser);

}
