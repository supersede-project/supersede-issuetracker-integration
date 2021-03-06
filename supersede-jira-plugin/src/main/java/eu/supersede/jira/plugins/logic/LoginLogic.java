/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eu.supersede.jira.plugins.logic;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.PropertySet;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.sal.api.pluginsettings.PluginSettings;

import eu.supersede.jira.plugins.activeobject.SupersedeLogin;
import eu.supersede.jira.plugins.activeobject.SupersedeLoginService;
import eu.supersede.jira.plugins.servlet.SupersedeCfg;
import static com.google.common.base.Preconditions.checkNotNull;

public class LoginLogic {

	private static LoginLogic logic;

	private static String authToken;

	private String serverUrl, username, password, tenantOverride, replanHost, replanTenant, similarity, feedback, feedbackUser, feedbackPassword;

	private String currentProject; // TODO: 20170803 No longer required, if "get
									// tenant from user group" is confirmed

	public static final int CONN_TIMEOUT = 10000;

	private static final Logger log = LoggerFactory.getLogger(LoginLogic.class);

	private static final String GROUP_TENANT_PREFIX = "supersede-tenant-";

	private static SupersedeLoginService ssLoginService;

	private LoginLogic() {
	}

	public static LoginLogic getInstance() {
		if (logic == null) {
			logic = new LoginLogic();
		}
		return logic;
	}

	private LoginLogic(SupersedeLoginService loginService) {
		ssLoginService = checkNotNull(loginService);
	}

	public static LoginLogic getInstance(SupersedeLoginService loginService) {
		if (logic == null || ssLoginService == null) {
			logic = new LoginLogic(loginService);
		}
		return logic;
	}

	public void loadConfiguration(PluginSettings settings) {
		PluginSettings pluginSettings = settings;

		String jiraUser = getCurrentUser().getUsername();
		SupersedeLogin ssLogin = ssLoginService.getLogin(jiraUser);

		if (ssLogin != null && ("".equals(ssLogin.getSSUser()) || ssLogin.getSSUser() == null)) {
			ssLogin = null;
		}

		serverUrl = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_HOSTNAME, SupersedeCfg.DEF_HOSTNAME);
		username = ssLogin != null ? ssLogin.getSSUser() : SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_USERNAME, SupersedeCfg.DEF_USERNAME);
		password = ssLogin != null ? ssLogin.getSSPassword() : SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_PASSWORD, SupersedeCfg.DEF_PASSWORD);
		tenantOverride = ssLogin != null ? ssLogin.getTenant() : SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_TENANT, SupersedeCfg.DEF_TENANT);
		replanHost = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_REPLAN_HOST, SupersedeCfg.DEF_REPLAN_HOST);
		replanTenant = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_REPLAN_TENANT, SupersedeCfg.DEF_REPLAN_TENANT);
		setSimilarity(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_SIMILARITY, SupersedeCfg.DEF_SIMILARITY));
		setFeedback(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_FEEDBACK, SupersedeCfg.DEF_FEEDBACK));
		setFeedbackUser(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_FEEDBACK_USER, SupersedeCfg.DEF_FEEDBACK_USER));
		setFeedbackPassword(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_FEEDBACK_PASSWORD, SupersedeCfg.DEF_FEEDBACK_PASSWORD));
	}
	
	public void loadConfigurationForWebHook(PluginSettings settings) {
		PluginSettings pluginSettings = settings;
		setFeedback(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_FEEDBACK, SupersedeCfg.DEF_FEEDBACK));
		setFeedbackUser(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_FEEDBACK_USER, SupersedeCfg.DEF_FEEDBACK_USER));
		setFeedbackPassword(SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_FEEDBACK_PASSWORD, SupersedeCfg.DEF_FEEDBACK_PASSWORD));
	}

	public String getBasicAuth() {
		String userpass = getUsername() + ":" + getPassword();
		String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
		return basicAuth;
	}

	public ApplicationUser getCurrentUser() {
		return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
	}

	/**
	 * 
	 * @return the session id for this login
	 * @throws Exception
	 */
	public String login() throws Exception {
		URL url = new URL(getUrl() + "/login");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONN_TIMEOUT);
		conn.setReadTimeout(CONN_TIMEOUT);
		conn.setRequestMethod("GET");
		// conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Authorization", getBasicAuth());
		conn.setRequestProperty("TenantId", getCurrentProject());

		// fuck the response... 404 is still fine for a login
		log.info("login: " + conn.getResponseCode());

		Map<String, List<String>> map = conn.getHeaderFields();
		List<String> cookies = map.get("Set-Cookie");

		String sessionId = null;
		for (String s : cookies) {
			String[] split = s.split("=");
			if (split.length > 1) {
				if (split[0].equalsIgnoreCase("session")) {
					sessionId = split[1].substring(0, split[1].indexOf(';'));
				}
			}
		}
		System.out.println("session id is " + sessionId);
		return sessionId;
	}

	public String authenticate(String sessionId) throws Exception {
		URL url = new URL(getUrl() + "/user");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONN_TIMEOUT);
		conn.setReadTimeout(CONN_TIMEOUT);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", getBasicAuth());
		conn.setRequestProperty("TenantId", getCurrentProject());
		conn.setRequestProperty("Cookie", "SESSION=" + sessionId);

		Map<String, List<String>> map = conn.getHeaderFields();
		List<String> cookies = map.get("Set-Cookie");

		// if (authToken == null || authToken.isEmpty()) {
		String xsrf = null;
		for (String s : cookies) {
			String[] split = s.split("=");
			if (split.length > 1) {
				if (split[0].equalsIgnoreCase("xsrf-token")) {
					xsrf = split[1].substring(0, split[1].indexOf(';'));
				}
			}
		}
		authToken = xsrf;
		System.out.println("XSRF token is " + xsrf);
		// }
		return authToken;
	}

	public String getCurrentProject() {
		// this should be set in the query: otherwise a project should be picked
		// up by the user
		// UserUtil util = ComponentAccessor.getUserUtil();
		// SortedSet<Group> groups =
		// util.getGroupsForUser(getCurrentUser().getName());
		// for (Group g : groups) {
		// if (g.getName().startsWith(GROUP_TENANT_PREFIX)) {
		// return g.getName().split("-")[2];
		// }
		// }

		// TODO: Provisional check in order to return the default cfg tenant
		// until extensive test
		return tenantOverride.length() > 0 ? tenantOverride : currentProject;

	}

	public String getUrl() {
		return "/".equals(serverUrl.substring(serverUrl.length() - 1)) ? serverUrl : serverUrl + "/";
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String getReplanHost() {
		return "/".equals(replanHost.substring(replanHost.length() - 1)) ? replanHost : replanHost + "/";
	}

	public String getReplanTenant() {
		return this.replanTenant;
	}

	public void setSessionCookie(HttpServletResponse res, String sessionId) {
		Cookie cookie = new Cookie("SESSION", sessionId);
		cookie.setPath("/");
		cookie.setMaxAge(60 * 60 * 24 * 365);
		res.addCookie(cookie);
	}

	public void setXsrfCookie(HttpServletResponse res, String xsrf) {
		Cookie cookie = new Cookie("XSRF-TOKEN", xsrf);
		cookie.setPath("/");
		cookie.setMaxAge(60 * 60 * 24 * 365);
		res.addCookie(cookie);
	}

	public String getSimilarity() {
		return similarity;
	}

	public void setSimilarity(String similarity) {
		this.similarity = similarity;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public String getFeedbackUser() {
		return feedbackUser;
	}

	public void setFeedbackUser(String feedbackUser) {
		this.feedbackUser = feedbackUser;
	}

	public String getFeedbackPassword() {
		return feedbackPassword;
	}

	public void setFeedbackPassword(String feedbackPassword) {
		this.feedbackPassword = feedbackPassword;
	}

}
