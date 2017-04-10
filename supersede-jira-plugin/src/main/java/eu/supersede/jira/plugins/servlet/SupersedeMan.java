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

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.json.JSONTokener;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import jdk.nashorn.internal.parser.JSONParser;

/**
 * 
 * @author matteo.pedrotti@deltainformatica.eu
 *
 */
public class SupersedeMan extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(SupersedeMan.class);

	/**
	 * Velocity model used to output the html
	 */
	private static final String MANAGER_BROWSER_TEMPLATE = "/templates/supersede-man.vm";

	/**
	 * This is the name of the custom SUPERSEDE issue field. The field is added
	 * by the plugin (if not already available) to all the issue types. The
	 * value of the field is the id of the SUPERSEDE requirement or feature so
	 * that the two can be synchronised.
	 */
	private final static String SUPERSEDE_FIELD_NAME = "Supersede";
	
	private static final int CONN_TIMEOUT = 10000; 

	private IssueService issueService;
	private ProjectService projectService;
	private SearchService searchService;
	private UserManager userManager;
	private TemplateRenderer templateRenderer;
	private final com.atlassian.jira.user.util.UserManager jiraUserManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final CustomFieldManager customFieldManager;
	
	private String serverUrl, username, password, tenantOverride;
	private String currentProject; //TODO!!!

	public SupersedeMan(IssueService issueService, ProjectService projectService, SearchService searchService,
			UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager,
			TemplateRenderer templateRenderer, PluginSettingsFactory pluginSettingsFactory,
			CustomFieldManager customFieldManager) {
		this.issueService = issueService;
		this.projectService = projectService;
		this.searchService = searchService;
		this.userManager = userManager;
		this.templateRenderer = templateRenderer;
		this.jiraUserManager = jiraUserManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.customFieldManager = customFieldManager;
		loadConfiguration();
	}
	
	private void loadConfiguration(){
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		serverUrl = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_HOSTNAME, SupersedeCfg.DEF_HOSTNAME);
		username = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_USERNAME, SupersedeCfg.DEF_USERNAME);
		password = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_PASSWORD, SupersedeCfg.DEF_PASSWORD);
		tenantOverride = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_TENANT, SupersedeCfg.DEF_TENANT);
	}

	private ApplicationUser getCurrentUser(HttpServletRequest req) {
		return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
	}

	/**
	 * Verifies the supersede field is available in the custom field manager. If
	 * not, creates and associates it with all the issue types currently
	 * available.
	 * 
	 * @throws Exception
	 */
	private void checkSupersedeField() throws Exception {
		CustomField supersedeField = customFieldManager.getCustomFieldObjectByName(SUPERSEDE_FIELD_NAME);
		if (null == supersedeField) {
			CustomFieldType textFieldType = customFieldManager
					.getCustomFieldType("com.atlassian.jira.plugin.system.customfieldtypes:textfield");
			CustomFieldSearcher fieldSearcher = customFieldManager
					.getCustomFieldSearcher("com.atlassian.jira.plugin.system.customfieldtypes:textsearcher");
			List<JiraContextNode> contexts = new ArrayList<JiraContextNode>();
			contexts.add(GlobalIssueContext.getInstance());
			IssueTypeManager issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager.class);
			Collection<IssueType> issueTypes = issueTypeManager.getIssueTypes();
			// add supersede to all issue types
			List<IssueType> myIssueTypes = new LinkedList<IssueType>();
			for (IssueType it : issueTypes) {
				log.debug(it.getId() + " ", it.getName());
				myIssueTypes.add(it);
			}
			customFieldManager.createCustomField(SUPERSEDE_FIELD_NAME, "SUPERSEDE powered issue", textFieldType,
					fieldSearcher, contexts, myIssueTypes);
			log.info("the supersede custom field has been installed");
		} else {
			log.info("the supersede custom field is already available");
		}
	}

	private String getBasicAuth() {
		String userpass = getUsername()+ ":" + getPassword();
		String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
		return basicAuth;
	}

	private String getCurrentProject() {
		//this should be set in the query: otherwise a project should be picked up by the user
		return tenantOverride.length()>0?tenantOverride:currentProject;
	}
	
	private String getUrl(){
		return this.serverUrl;
	}
	
	private String getUsername(){
		return this.username;
	}
	
	private String getPassword(){
		return this.password;
	}
	
	/**
	 * 
	 * @return the session id for this login
	 * @throws Exception
	 */
	private String login() throws Exception {
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

	private String authenticate(String sessionId) throws Exception {
		URL url = new URL(getUrl() + "/user");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONN_TIMEOUT);
		conn.setReadTimeout(CONN_TIMEOUT);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", getBasicAuth());
		conn.setRequestProperty("TenantId", getCurrentProject());
		conn.setRequestProperty("Cookie", "SESSION="+sessionId);

		Map<String, List<String>> map = conn.getHeaderFields();
		List<String> cookies = map.get("Set-Cookie");

		String xsrf = null;
		for (String s : cookies) {
			String[] split = s.split("=");
			if (split.length > 1) {
				if (split[0].equalsIgnoreCase("xsrf-token")) {
					xsrf = split[1].substring(0, split[1].indexOf(';'));
				}
			}
		}

		System.out.println("XSRF token is " + xsrf);
		return xsrf;
	}

	private void fetchRequirements(String sessionId, Collection<Requirement> requirements) {
		try {

			URL url = new URL(getUrl() + "/supersede-dm-app/requirement");
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(CONN_TIMEOUT);
			conn.setReadTimeout(CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization", getBasicAuth());
			conn.setRequestProperty("TenantId", getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			log.debug("connection code " + conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				// System.out.println(output);
				sb.append(output);
			}
			JSONArray jarr = new JSONArray(sb.toString());
			// log.debug(jarr.toString(2));

			int l = jarr.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = jarr.getJSONObject(i);
				try {
					Requirement r = new Requirement(o);
					requirements.add(r);
				} catch (JSONException e) {
					log.error("parsing ", o);
				}
			}

			conn.disconnect();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private void getRequirements(Collection<Requirement> requirements) {
		try {
			String sessionId = login();
			fetchRequirements(sessionId, requirements);
		} catch (Exception e) {
			log.error("login error : " + e);
			return;
		}
	}

	/**
	 * Retrieve the issues with a valid supersede field set
	 * 
	 * @param req
	 * @return
	 */
	private List<Issue> getIssues(HttpServletRequest req) {
		// User is required to carry out a search
		ApplicationUser user = getCurrentUser(req);

		// search issues

		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();
		Query query = jqlClauseBuilder.field(SUPERSEDE_FIELD_NAME).isNot().empty().and().project(getCurrentProject())
				.buildQuery();
		// A page filter is used to provide pagination. Let's use an unlimited
		// filter to
		// to bypass pagination.
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		return searchResults.getIssues();
	}
	
	private Requirement fetchRequirement(String sessionId, String id) {
		try {

			URL url = new URL(getUrl() + "/supersede-dm-app/requirement/"+id);
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(CONN_TIMEOUT);
			conn.setReadTimeout(CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			//conn.setRequestProperty("Authorization", getBasicAuth());
			conn.setRequestProperty("TenantId", getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			log.debug("connection code " + conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				// System.out.println(output);
				sb.append(output);
			}
			JSONObject job = new JSONObject(sb.toString());
			log.debug(job.toString(2));
			
			Requirement r = new Requirement(job);
			
			conn.disconnect();
			return r;
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}
	
	private Requirement getRequirement(String id){
		try {
			String sessionId = login();
			return fetchRequirement(sessionId, id);
		} catch (Exception e) {
			log.error("login error : " + e);
			return null;
		}
	}

	private void newIssue(HttpServletRequest req, Collection<String> errors) {
		ApplicationUser user = getCurrentUser(req);
		// Perform creation if the "new" param is passed in
		// First we need to validate the new issue being created
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		// We're only going to set the summary and description. The rest are
		// hard-coded to
		// simplify this tutorial.
		issueInputParameters.setSummary(req.getParameter("name"));
		issueInputParameters.setDescription(req.getParameter("description"));
		CustomField supersedeField = customFieldManager.getCustomFieldObjectByName(SUPERSEDE_FIELD_NAME);
		issueInputParameters.addCustomFieldValue(supersedeField.getId(), req.getParameter("id"));

		// We need to set the assignee, reporter, project, and issueType...
		// For assignee and reporter, we'll just use the currentUser
		// issueInputParameters.setAssigneeId(user.getName());
		issueInputParameters.setReporterId(user.getName());
		// We hard-code the project name to be the project with the TUTORIAL key
		Project project = projectService.getProjectByKey(user, getCurrentProject().toUpperCase()).getProject();
		if (null == project) {
			errors.add("Cannot add issue for requirement " + req.getParameter("id") + ": no such project "
					+ getCurrentProject());
		} else {
			issueInputParameters.setProjectId(project.getId());
			// We also hard-code the issueType to be a "bug" == 1
			issueInputParameters.setIssueTypeId(project.getIssueTypes().iterator().next().getId());
			// Perform the validation
			issueInputParameters.setSkipScreenCheck(true);
			IssueService.CreateValidationResult result = issueService.validateCreate(user, issueInputParameters);

			if (result.getErrorCollection().hasAnyErrors()) {
				Map<String, String> errorsMap = result.getErrorCollection().getErrors();
				for (String eid : errorsMap.keySet()) {
					errors.add(eid + ": " + errorsMap.get(eid));
				}
				log.error("cannot add issue for requirement " + req.getParameter("id"));
			} else {
				IssueResult issue = issueService.create(user, result);
				log.info("added issue for requirement " + req.getParameter("id"));

				/*
				 * issue.getIssue().setCustomFieldValue(supersedeField,
				 * req.getParameter("id")); Object customField =
				 * issue.getIssue().getCustomFieldValue(supersedeField);
				 * log.debug("custom field: ",customField);
				 * 
				 * issue.getIssue().store();
				 * 
				 * IssueInputParameters updates =
				 * issueService.newIssueInputParameters();
				 * issueInputParameters.addCustomFieldValue(supersedeField.getId
				 * (), req.getParameter("id"));
				 * issueInputParameters.addCustomFieldValue(
				 * SUPERSEDE_FIELD_NAME, req.getParameter("id"));
				 * updates.addCustomFieldValue(supersedeField.getId(),
				 * req.getParameter("id"));
				 * updates.addCustomFieldValue(SUPERSEDE_FIELD_NAME,
				 * req.getParameter("id")); IssueService.UpdateValidationResult
				 * updateRes = issueService.validateUpdate(user,
				 * issue.getIssue().getId(), updates);
				 * 
				 * if (updateRes.getErrorCollection().hasAnyErrors()) {
				 * Map<String,String> errorsMap =
				 * updateRes.getErrorCollection().getErrors(); for(String eid :
				 * errorsMap.keySet()){ errors.add(eid+": "+errorsMap.get(eid));
				 * } log.error("cannot update issue for requirement "
				 * +req.getParameter("id")); }else{ IssueResult updated =
				 * issueService.update(user, updateRes); log.info(
				 * "updated issue for requirement "+req.getParameter("id"));
				 * 
				 * Object updatedField =
				 * updated.getIssue().getCustomFieldValue(supersedeField);
				 * log.debug("updated custom field: ",updatedField); }
				 */
			}
		}
	}

	private void sendPostRequest(String sessionId, String xsrf, String name, String description) {

		try{

			URL url = new URL(getUrl() + "/supersede-dm-app/requirement");
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(CONN_TIMEOUT);
			conn.setReadTimeout(CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			//conn.setRequestProperty("Authorization", getBasicAuth());
			conn.setRequestProperty("TenantId", getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			//conn.setRequestProperty("SESSION", sessionId);
			conn.setRequestProperty("X-XSRF-TOKEN", xsrf);

			JSONObject req = new JSONObject();
			req.put("name", name);
			req.put("description", description);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(req.toString());
			wr.flush();

			log.debug("connection code " + conn.getResponseCode());
			BufferedReader ebr = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
			log.debug("printing output:");
			String error;
			try {
				while ((error = ebr.readLine()) != null) {
					System.err.println(error);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			log.debug("printing output:");
			String output;
			StringBuffer sb = new StringBuffer();
			try {
				while ((output = br.readLine()) != null) {
					System.out.println(output);
					sb.append(output);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			conn.disconnect();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private void testCreateRequirement(String sessionid, String xsrf, Issue issue) {
		log.debug("creating requirement for issue " + issue.getId());
		// 1. send the request to supersede
		sendPostRequest(sessionid, xsrf, issue.getSummary(), issue.getDescription());
		// 2. get the response with the requirement id
		// 3. update the issue with the custom field "Supersede" set as the
		// requirement id
	}

	private void newRequirement(HttpServletRequest req, Collection<String> errors) {
		String issueKey = req.getParameter("issuekey");
		log.info("creating new requirement for " + issueKey);
		IssueResult issueRes = issueService.getIssue(getCurrentUser(req), issueKey);
		if (issueRes.isValid()) {
			try {
				String sessionId = login();
				String xsrf = authenticate(sessionId);
				testCreateRequirement(sessionId, xsrf, issueRes.getIssue());
			} catch (Exception e) {
				log.error("login error: " + e);
			}
		} else {
			errors.add("invalid issue key " + issueKey);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// process request
		List<String> errors = new LinkedList<String>();
		if ("y".equals(req.getParameter("import"))) {
			errors.add("importing " + req.getParameter("id"));
			newIssue(req, errors);
		} else if ("y".equals(req.getParameter("export"))) {
			errors.add("exporting " + req.getParameter("issuekey"));
			newRequirement(req, errors);
		}
		// ---

		try {
			checkSupersedeField();
		} catch (Exception e) {
			log.error("checking custom supersede field: " + e);
		}
		// Render the list of issues (list.vm) if no params are passed in
		List<Issue> issues = getIssues(req);
		List<Requirement> requirements = new LinkedList<Requirement>();
		getRequirements(requirements);
		Map<String, Object> context = Maps.newHashMap();
		context.put("issues", issues);
		context.put("requirements", requirements);
		context.put("errors", errors);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		context.put("customFieldManager", customFieldManager);
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render(MANAGER_BROWSER_TEMPLATE, context, resp.getWriter());
	}

}