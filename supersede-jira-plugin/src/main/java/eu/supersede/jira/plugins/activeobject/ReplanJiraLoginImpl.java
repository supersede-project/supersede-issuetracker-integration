package eu.supersede.jira.plugins.activeobject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.ProcessLogic;
import eu.supersede.jira.plugins.servlet.Alert;
import net.java.ao.Query;

@Scanned
@Named
public class ReplanJiraLoginImpl implements ReplanJiraLoginService {

	private final ActiveObjects ao;

	ProcessLogic processLogic = ProcessLogic.getInstance();

	@Inject
	public ReplanJiraLoginImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}

	@Override
	public ReplanJiraLogin add(String replanUsername, String jiraUsername) {

		final ReplanJiraLogin login = ao.create(ReplanJiraLogin.class);
		login.setJiraUsername(jiraUsername);
		login.setReplanUsername(replanUsername);
		login.save();
		return login;
	}

	@Override
	public List<ReplanJiraLogin> getAllLogins() {
		return newArrayList(ao.find(ReplanJiraLogin.class));
	}
	
	@Override
	public ReplanJiraLogin getLoginByJiraUsername(String jiraUsername) {
		ReplanJiraLogin[] result = ao.find(ReplanJiraLogin.class, Query.select().where("JIRA_USERNAME LIKE ?", jiraUsername));
		if(result != null && result.length > 0) {
			return result[0];
		}
		return null;
	}
	
	@Override
	public ReplanJiraLogin getLoginByReplanUsername(String replanUsername) {
		ReplanJiraLogin[] result = ao.find(ReplanJiraLogin.class, Query.select().where("REPLAN_USERNAME LIKE ?", replanUsername));
		if(result != null && result.length > 0) {
			return result[0];
		}
		return null;
	}

}
