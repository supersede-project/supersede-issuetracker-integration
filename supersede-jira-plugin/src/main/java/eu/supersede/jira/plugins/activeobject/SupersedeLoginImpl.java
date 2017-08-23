package eu.supersede.jira.plugins.activeobject;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import net.java.ao.Query;

@Scanned
@Named
public class SupersedeLoginImpl implements SupersedeLoginService {

	private final ActiveObjects ao;

	@Inject
	public SupersedeLoginImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}

	@Override
	public SupersedeLogin add(String jiraUser, String ssUser, String ssPassword, String tenant) {
		final SupersedeLogin login = ao.create(SupersedeLogin.class);
		login.setJiraUser(jiraUser);
		login.setSSUser(ssUser);
		login.setSSPassword(ssPassword);
		login.save();
		return login;
	}

	@Override
	public SupersedeLogin update(SupersedeLogin source, String jiraUser, String ssUser, String ssPassword, String tenant) {
		SupersedeLogin login = ao.get(SupersedeLogin.class, source.getID());
		login.setJiraUser(jiraUser);
		login.setSSUser(ssUser);
		login.setSSPassword(ssPassword);
		login.save();
		return login;
	}

	@Override
	public SupersedeLogin getLogin(String jiraUser) {
		SupersedeLogin[] result = ao.find(SupersedeLogin.class, Query.select().where("JIRA_USER LIKE ?", jiraUser));
		if (result != null && result.length > 0) {
			return result[0];
		}
		return null;
	}

}
