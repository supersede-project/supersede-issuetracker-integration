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
package eu.supersede.jira.plugins.servlet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.application.api.Application;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import eu.supersede.jira.plugins.activeobject.ReplanJiraLogin;
import eu.supersede.jira.plugins.activeobject.ReplanJiraLoginService;
import eu.supersede.jira.plugins.activeobject.SupersedeLogin;
import eu.supersede.jira.plugins.activeobject.SupersedeLoginService;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.ReplanLogic;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

/**
 * 
 * @author matteo.pedrotti@deltainformatica.eu
 *
 */
public class SupersedeCfg extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8883489846712728448L;

	private static final Logger log = LoggerFactory.getLogger(SupersedeCfg.class);

	/**
	 * Velocity model used to output the html
	 */
	private static final String CONFIG_BROWSER_TEMPLATE = "/templates/supersede-cfg.vm";

	public static final String KEY_HOSTNAME = "hostname", KEY_USERNAME = "username", KEY_PASSWORD = "password", KEY_TENANT = "tenant", KEY_REPLAN_HOST = "replan-host", KEY_REPLAN_TENANT = "replan-tenant";

	public static final String DEF_HOSTNAME = "http://localhost", DEF_USERNAME = "admin", DEF_PASSWORD = "admin", DEF_TENANT = "", DEF_REPLAN_HOST = "", DEF_REPLAN_TENANT = "";

	private UserManager userManager;
	private TemplateRenderer templateRenderer;
	private final com.atlassian.jira.user.util.UserManager jiraUserManager;
	private final PluginSettingsFactory pluginSettingsFactory;

	private final SupersedeLoginService ssLoginService;
	private final UserSearchService userSearchService;
	private final ReplanJiraLoginService replanJiraLoginService;
	private final LoginLogic loginLogic;

	public SupersedeCfg(UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer, PluginSettingsFactory pluginSettingsFactory, SupersedeLoginService ssLoginService,
			UserSearchService userSearchService, ReplanJiraLoginService replanJiraLoginService) {
		this.userManager = userManager;
		this.templateRenderer = templateRenderer;
		this.jiraUserManager = jiraUserManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.ssLoginService = checkNotNull(ssLoginService);
		this.userSearchService = userSearchService;
		this.replanJiraLoginService = checkNotNull(replanJiraLoginService);

		loginLogic = LoginLogic.getInstance(ssLoginService);
		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());
	}

	/**
	 * retrieve the actual storage key for any given locally scoped key
	 * 
	 * @param key
	 * @return
	 */
	public static String getStorageKey(String key) {
		return SupersedeCfg.class.getPackage().getName() + "." + key;
	}

	/**
	 * Retrieve the key stored in the given plugin settings
	 * 
	 * @param pluginSettings
	 *            an instance of the pluginSettings to use. Use the
	 *            settingsFactory.createGlobalSettings() to retrieve it.
	 * @param key
	 *            the locally scoped key, just the name of the variable. The
	 *            actual stored key is package dependent but it's name is
	 *            handled automatically.
	 * @param defaultValue
	 *            if no such value exists, the default value is set in the
	 *            settings and returned
	 * @return
	 */
	public static String getConfigurationValue(PluginSettings pluginSettings, String key, String defaultValue) {
		final String k = getStorageKey(key);
		String v = (String) pluginSettings.get(k);
		if (null == v) {
			v = defaultValue;
			pluginSettings.put(k, v);
		}
		return v;
	}

	/**
	 * Forcefully set the given key/value pair in the pluginSettings
	 * 
	 * @param pluginSettings
	 * @param key
	 *            a locally scoped key. The actual storage key is handled
	 *            internally
	 * @param value
	 * @return
	 */
	public static void setConfigurationValue(PluginSettings pluginSettings, String key, String value) {
		final String k = getStorageKey(key);
		pluginSettings.put(k, value);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("y".equals(req.getParameter("config"))) {
			PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
			pluginSettings.put(getStorageKey(KEY_HOSTNAME), req.getParameter(KEY_HOSTNAME));
			pluginSettings.put(getStorageKey(KEY_USERNAME), req.getParameter(KEY_USERNAME));
			// only set the password if set
			String pwd = req.getParameter(KEY_PASSWORD);
			if (null != pwd && pwd.trim().length() > 0) {
				pluginSettings.put(getStorageKey(KEY_PASSWORD), pwd);
			}
		} else if ("y".equals(req.getParameter("options"))) {
			PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
			pluginSettings.put(getStorageKey(KEY_TENANT), req.getParameter(KEY_TENANT));
		} else if ("y".equals(req.getParameter("replan"))) {
			PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
			pluginSettings.put(getStorageKey(KEY_REPLAN_HOST), req.getParameter(KEY_REPLAN_HOST));
			pluginSettings.put(getStorageKey(KEY_REPLAN_TENANT), req.getParameter(KEY_REPLAN_TENANT));
		} else if ("y".equals(req.getParameter("replan-login-table"))) {
			String jiraUsername = req.getParameter("jira-user");
			String replanUsername = req.getParameter("replan-user");
			if (jiraUsername == null || replanUsername == null || jiraUsername.isEmpty() || replanUsername.isEmpty()) {
				resp.sendRedirect("supersede-cfg");
				return;
			}

			ReplanJiraLogin login = replanJiraLoginService.getLoginByJiraUsername(jiraUsername, loginLogic.getReplanTenant());
			// if (login == null) {
			// login =
			// replanJiraLoginService.getLoginByReplanUsername(replanUsername,
			// loginLogic.getReplanTenant());
			// }

			if (login != null) {
				login.setJiraUsername(jiraUsername);
				login.setReplanUsername(replanUsername.replace(' ', '_'));
				login.save();
			} else {
				replanJiraLoginService.add(replanUsername, jiraUsername, loginLogic.getReplanTenant());
			}
		} else if ("y".equals(req.getParameter("SSlogin"))) {
			String ssUser = req.getParameter("SSusername");
			String ssPass = req.getParameter("SSpassword");
			String ssTenant = req.getParameter("SStenant");
			String jiraUser = loginLogic.getCurrentUser().getUsername();
			SupersedeLogin ssLogin = ssLoginService.getLogin(jiraUser);
			if (ssLogin != null) {
				ssLoginService.update(ssLogin, jiraUser, ssUser, ssPass, ssTenant);
			}
			ssLoginService.add(jiraUser, ssUser, ssPass, ssTenant);

		}
		resp.sendRedirect("supersede-cfg");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// what request?
		List<String> errors = new LinkedList<String>();

		// get parameters
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		String hostSetting = getConfigurationValue(pluginSettings, KEY_HOSTNAME, DEF_HOSTNAME);
		String usernameSetting = getConfigurationValue(pluginSettings, KEY_USERNAME, DEF_USERNAME);
		String tenantSetting = getConfigurationValue(pluginSettings, KEY_TENANT, DEF_TENANT);
		String replanSetting = getConfigurationValue(pluginSettings, KEY_REPLAN_HOST, DEF_REPLAN_HOST);
		String replanTenantSetting = getConfigurationValue(pluginSettings, KEY_REPLAN_TENANT, DEF_REPLAN_TENANT);

		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());

		Map<String, Object> context = Maps.newHashMap();
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		context.put("errors", errors);
		context.put(KEY_HOSTNAME, hostSetting);
		context.put(KEY_USERNAME, usernameSetting);
		context.put(KEY_TENANT, tenantSetting);
		context.put(KEY_REPLAN_HOST, replanSetting);
		context.put(KEY_REPLAN_TENANT, replanTenantSetting);

		String jiraUser = loginLogic.getCurrentUser().getUsername();
		SupersedeLogin ssLogin = ssLoginService.getLogin(jiraUser);
		context.put("ssUser", ssLogin != null ? ssLogin.getSSUser() : "");
		context.put("ssTenant", ssLogin != null ? ssLogin.getTenant() : "");

		// get all replan users
		context.put("replanLogins", replanJiraLoginService.getAllLogins());

		// Pass a list of JIRA Users
		List<ApplicationUser> activeUsers = userSearchService.findUsersAllowEmptyQuery(new JiraServiceContextImpl(loginLogic.getCurrentUser()), "");

		// Get a list of tenant users
		ReplanLogic replanLogic = ReplanLogic.getInstance();
		List<String> replanUsers = replanLogic.getReplanUsersByTenant();

		context.put("users", activeUsers);
		context.put("replanUsers", replanUsers);
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render(CONFIG_BROWSER_TEMPLATE, context, resp.getWriter());
	}

}