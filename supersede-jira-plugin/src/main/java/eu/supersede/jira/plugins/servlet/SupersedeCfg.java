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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

/**
 * 
 * @author matteo.pedrotti@deltainformatica.eu
 *
 */
public class SupersedeCfg extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(SupersedeCfg.class);

	/**
	 * Velocity model used to output the html
	 */
	private static final String CONFIG_BROWSER_TEMPLATE = "/templates/supersede-cfg.vm";
	
	public static final String 
			KEY_HOSTNAME = "hostname",
			KEY_USERNAME = "username",
			KEY_PASSWORD = "password",
			KEY_TENANT = "tenant";
	
	public static final String 
			DEF_HOSTNAME = "http://localhost",
			DEF_USERNAME = "admin",
			DEF_PASSWORD = "admin",
			DEF_TENANT = "";
	
	private UserManager userManager;
	private TemplateRenderer templateRenderer;
	private final com.atlassian.jira.user.util.UserManager jiraUserManager;
	private final PluginSettingsFactory pluginSettingsFactory;

	public SupersedeCfg(UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager,
			TemplateRenderer templateRenderer, PluginSettingsFactory pluginSettingsFactory) {
		this.userManager = userManager;
		this.templateRenderer = templateRenderer;
		this.jiraUserManager = jiraUserManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
	}
	
	/**
	 * retrieve the actual storage key for any given locally scoped key
	 * @param key
	 * @return
	 */
	public static String getStorageKey(String key){
		return SupersedeCfg.class.getPackage().getName()+"."+key;
	}
	
	/**
	 * Retrieve the key stored in the given plugin settings
	 * @param pluginSettings an instance of the pluginSettings to use. Use the settingsFactory.createGlobalSettings() to retrieve it.
	 * @param key the locally scoped key, just the name of the variable. The actual stored key is package dependent but it's name is handled automatically.
	 * @param defaultValue if no such value exists, the default value is set in the settings and returned
	 * @return
	 */
	public static String getConfigurationValue(PluginSettings pluginSettings, String key, String defaultValue){
		final String k = getStorageKey(key);
		String v = (String)pluginSettings.get(k);
		if(null == v){
			v = defaultValue;
			pluginSettings.put(k, v);
		}
		return v;
	}
	
	/**
	 * Forcefully set the given key/value pair in the pluginSettings
	 * @param pluginSettings
	 * @param key a locally scoped key. The actual storage key is handled internally
	 * @param value
	 * @return
	 */
	public static void setConfigurationValue(PluginSettings pluginSettings, String key, String value){
		final String k = getStorageKey(key);
		pluginSettings.put(k, value);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException ,IOException {
		if ("y".equals(req.getParameter("config"))) {
			PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
			pluginSettings.put(getStorageKey(KEY_HOSTNAME), req.getParameter(KEY_HOSTNAME));
			pluginSettings.put(getStorageKey(KEY_USERNAME), req.getParameter(KEY_USERNAME));
			//only set the password if set
			String pwd = req.getParameter(KEY_PASSWORD);
			if(null != pwd && pwd.trim().length()>0){
				pluginSettings.put(getStorageKey(KEY_PASSWORD), pwd);
			}
		} else if ("y".equals(req.getParameter("options"))) {
			PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
			pluginSettings.put(getStorageKey(KEY_TENANT), req.getParameter(KEY_TENANT));
		}
		resp.sendRedirect("supersede-cfg");
	}
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
    	//what request?
		List<String> errors = new LinkedList<String>();
		
		//get parameters
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		String hostSetting = getConfigurationValue(pluginSettings, KEY_HOSTNAME, DEF_HOSTNAME);
		String usernameSetting = getConfigurationValue(pluginSettings, KEY_USERNAME, DEF_USERNAME);
		String tenantSetting = getConfigurationValue(pluginSettings, KEY_TENANT, DEF_TENANT);

		Map<String, Object> context = Maps.newHashMap();
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		context.put("errors", errors);
		context.put(KEY_HOSTNAME, hostSetting);
		context.put(KEY_USERNAME, usernameSetting);
		context.put(KEY_TENANT, tenantSetting);
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render(CONFIG_BROWSER_TEMPLATE, context, resp.getWriter());
    }

}