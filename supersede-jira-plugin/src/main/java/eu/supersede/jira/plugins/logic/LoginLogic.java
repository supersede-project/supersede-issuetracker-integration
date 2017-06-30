package eu.supersede.jira.plugins.logic;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.pluginsettings.PluginSettings;

import eu.supersede.jira.plugins.servlet.SupersedeCfg;

public class LoginLogic {

	private static LoginLogic logic;

	private static String authToken;

	private String serverUrl, username, password, tenantOverride;

	private String currentProject; // TODO!!!

	public static final int CONN_TIMEOUT = 10000;

	private static final Logger log = LoggerFactory.getLogger(LoginLogic.class);

	private LoginLogic() {
	}

	public static LoginLogic getInstance() {
		if (logic == null) {
			logic = new LoginLogic();
		}
		return logic;
	}

	public void loadConfiguration(PluginSettings settings) {
		PluginSettings pluginSettings = settings;
		serverUrl = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_HOSTNAME,
				SupersedeCfg.DEF_HOSTNAME);
		username = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_USERNAME,
				SupersedeCfg.DEF_USERNAME);
		password = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_PASSWORD,
				SupersedeCfg.DEF_PASSWORD);
		tenantOverride = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_TENANT,
				SupersedeCfg.DEF_TENANT);
	}

	public String getBasicAuth() {
		String userpass = getUsername() + ":" + getPassword();
		String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
		return basicAuth;
	}

	public ApplicationUser getCurrentUser(HttpServletRequest req) {
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

		if (authToken == null || authToken.isEmpty()) {
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
		}
		return authToken;
	}

	public String getCurrentProject() {
		// this should be set in the query: otherwise a project should be picked
		// up by the user
		return tenantOverride.length() > 0 ? tenantOverride : currentProject;
	}

	public String getUrl() {
		return this.serverUrl;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

}
