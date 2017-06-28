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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
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
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.query.Query;
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
	private final static String SUPERSEDE_FIELD_NAME = "Supersede",
			SUPERSEDE_FIELD_TYPE = "eu.supersede.jira.plugins.supersede-jira-plugin:supersede-custom-field",
			CUSTOM_FIELD_SEARCHER = "com.atlassian.jira.plugin.system.customfieldtypes:textsearcher";

	// STATIC CUSTOM STRING AND FIELDS

	private static final String SEPARATOR = "\\n";
	private static final String PARAM_ACTION = "action";
	private static final String PARAM_SELECTION_LIST = "selectionList";
	private static final String PARAM_ISSUES_SELECTION_LIST = "issuesSelectionList";

	// END CUSTOM STRING AND FIELDS SECTION

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
	private Long supersedeFieldId;
	private String currentProject; // TODO!!!

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

	private void loadConfiguration() {
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		serverUrl = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_HOSTNAME,
				SupersedeCfg.DEF_HOSTNAME);
		username = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_USERNAME,
				SupersedeCfg.DEF_USERNAME);
		password = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_PASSWORD,
				SupersedeCfg.DEF_PASSWORD);
		tenantOverride = SupersedeCfg.getConfigurationValue(pluginSettings, SupersedeCfg.KEY_TENANT,
				SupersedeCfg.DEF_TENANT);
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
		CustomFieldType supersedeFieldType = getSupersedeCustomFieldType();
		CustomField supersedeField = getSupersedeCustomField(supersedeFieldType);
		if (null == supersedeField) {
			CustomFieldSearcher fieldSearcher = customFieldManager.getCustomFieldSearcher(CUSTOM_FIELD_SEARCHER);
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
			supersedeField = customFieldManager.createCustomField(SUPERSEDE_FIELD_NAME, "SUPERSEDE powered issue",
					supersedeFieldType, fieldSearcher, contexts, myIssueTypes);
			log.info("the supersede custom field has been installed to all the issue types");
		} else {
			log.info("the supersede custom field is already available");
		}
		supersedeFieldId = supersedeField.getIdAsLong();
		log.debug("supersede custom field id is " + supersedeFieldId);
	}

	private String getBasicAuth() {
		String userpass = getUsername() + ":" + getPassword();
		String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
		return basicAuth;
	}

	private String getCurrentProject() {
		// this should be set in the query: otherwise a project should be picked
		// up by the user
		return tenantOverride.length() > 0 ? tenantOverride : currentProject;
	}

	private String getUrl() {
		return this.serverUrl;
	}

	private String getUsername() {
		return this.username;
	}

	private String getPassword() {
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
		conn.setRequestProperty("Cookie", "SESSION=" + sessionId);

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

	/**
	 * Remove all the requirements which are already mapped as issues
	 * 
	 * @param user
	 * @param requirements
	 */
	private void filterRequirements(ApplicationUser user, Collection<Requirement> requirements) {
		for (Iterator<Requirement> ir = requirements.iterator(); ir.hasNext();) {
			Requirement r = ir.next();
			Issue i = getIssueByRequirement(user, r.getId());
			if (null != i) {
				ir.remove();
				log.debug("removed requirement " + r.getId() + " because already mapped to " + i.getKey());
			}
		}
	}

	private void getRequirements(HttpServletRequest req, Collection<Requirement> requirements) {
		getRequirements(req, requirements, true);
	}

	private void getRequirements(HttpServletRequest req, Collection<Requirement> requirements, boolean filter) {
		try {
			ApplicationUser user = getCurrentUser(req);
			String sessionId = login();
			fetchRequirements(sessionId, requirements);
			if (filter) {
				filterRequirements(user, requirements);
			}
		} catch (Exception e) {
			log.error("login error : " + e);
			return;
		}
	}

	private List<Issue> getIssues(HttpServletRequest req) {
		return getIssues(req, null);
	}

	/**
	 * Retrieve the issues with a valid supersede field set
	 * 
	 * @param req
	 * @return
	 */
	private List<Issue> getIssues(HttpServletRequest req, String id) {
		// User is required to carry out a search
		ApplicationUser user = getCurrentUser(req);

		// search issues

		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();

		// Build the basic Jql query
		jqlClauseBuilder.customField(supersedeFieldId).isNotEmpty().and().project(getCurrentProject());
		if (id != null) {
			// if an ID is provided, use in in filter
			jqlClauseBuilder.and().customField(supersedeFieldId).like(id);
		}
		Query query = jqlClauseBuilder.buildQuery();
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

	private Issue getIssueByRequirement(ApplicationUser user, String requirementId) {
		// search issues
		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();
		Query query = jqlClauseBuilder.customField(supersedeFieldId).like(requirementId).and()
				.project(getCurrentProject()).buildQuery();
		log.debug(query.getQueryString());
		log.debug(query.getWhereClause().toString());
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
		List<Issue> issues = searchResults.getIssues();
		if (0 == issues.size()) {
			log.debug("no issues found for requirement " + requirementId);
			return null;
		}
		if (1 < issues.size()) {
			log.warn("more issues mapped to the same requirement " + requirementId + ": returning the first found");
		}
		return issues.get(0);
	}

	private Requirement fetchRequirement(String sessionId, String id) {
		try {

			URL url = new URL(getUrl() + "/supersede-dm-app/requirement/" + id);
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(CONN_TIMEOUT);
			conn.setReadTimeout(CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			// conn.setRequestProperty("Authorization", getBasicAuth());
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

	private Requirement getRequirement(String id) {
		try {
			String sessionId = login();
			return fetchRequirement(sessionId, id);
		} catch (Exception e) {
			log.error("login error : " + e);
			return null;
		}
	}

	private CustomFieldType getSupersedeCustomFieldType() {
		CustomFieldType supersedeFieldType = customFieldManager.getCustomFieldType(SUPERSEDE_FIELD_TYPE);
		if (null == supersedeFieldType) {
			log.error("no such custom field type found: " + SUPERSEDE_FIELD_TYPE);
			for (CustomFieldType t : customFieldManager.getCustomFieldTypes()) {
				log.debug(t.getName() + " " + t.getKey());
			}
			throw new NullPointerException("no " + SUPERSEDE_FIELD_TYPE + " custom field available");
		}
		return supersedeFieldType;
	}

	private CustomField getSupersedeCustomField(CustomFieldType supersedeFieldType) {
		CustomField supersedeField = null;
		Collection<CustomField> supersedeFields = customFieldManager.getCustomFieldObjectsByName(SUPERSEDE_FIELD_NAME);
		for (CustomField cf : supersedeFields) {
			if (cf.getCustomFieldType().equals(supersedeFieldType)) {
				supersedeField = cf;
			}
		}
		return supersedeField;
	}

	private void newIssue(HttpServletRequest req, Collection<String> errors) {
		newIssue(req, req.getParameter("name"), req.getParameter("description"), req.getParameter("id"), errors);
	}

	private void newIssue(HttpServletRequest req, String name, String description, String id,
			Collection<String> errors) {
		ApplicationUser user = getCurrentUser(req);
		// Perform creation if the "new" param is passed in
		// First we need to validate the new issue being created
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		// We're only going to set the summary and description. The rest are
		// hard-coded to
		// simplify this tutorial.
		issueInputParameters.setSummary(name);
		issueInputParameters.setDescription(description);
		CustomField supersedeField = getSupersedeCustomField(getSupersedeCustomFieldType());
		issueInputParameters.addCustomFieldValue(supersedeField.getId(), id);

		// We need to set the assignee, reporter, project, and issueType...
		// For assignee and reporter, we'll just use the currentUser
		// issueInputParameters.setAssigneeId(user.getName());
		issueInputParameters.setReporterId(user.getName());
		// We hard-code the project name to be the project with the TUTORIAL key
		Project project = projectService.getProjectByKey(user, getCurrentProject().toUpperCase()).getProject();
		if (null == project) {
			errors.add("Cannot add issue for requirement " + id + ": no such project "
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
				log.error("cannot add issue for requirement " + id);
			} else {
				IssueResult issue = issueService.create(user, result);
				log.info("added issue for requirement " + id);
			}
		}
	}

	/**
	 * Perform a REST call (POST) asking SUPERSEDE to create a new requirement
	 * with the given name and description.
	 * 
	 * @param sessionId
	 *            current user session identifier
	 * @param xsrf
	 *            the authentication token to be used for secured methods
	 * @param name
	 *            the requirement name
	 * @param description
	 *            the requirement description
	 * @return the id of the created requirement in SUPERSEDE
	 */
	private String sendPostRequest(String sessionId, String xsrf, String name, String description) {
		String requirementId = null;
		try {

			URL url = new URL(getUrl() + "/supersede-dm-app/requirement");
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(CONN_TIMEOUT);
			conn.setReadTimeout(CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			// conn.setRequestProperty("Authorization", getBasicAuth());
			conn.setRequestProperty("TenantId", getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			// conn.setRequestProperty("SESSION", sessionId);
			conn.setRequestProperty("X-XSRF-TOKEN", xsrf);

			JSONObject req = new JSONObject();
			req.put("name", name);
			req.put("description", description);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(req.toString());
			wr.flush();

			log.debug("connection code " + conn.getResponseCode());
			String locationHeader = conn.getHeaderField("Location");
			log.debug("location header " + locationHeader);
			requirementId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
			log.debug("requirement id " + requirementId);

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
		return requirementId;
	}

	private void testCreateRequirement(String sessionid, String xsrf, MutableIssue issue, ApplicationUser user,
			Collection<String> errors) {
		log.debug("creating requirement for issue " + issue.getId());
		// 1. send the request to supersede
		String requirementId = sendPostRequest(sessionid, xsrf, issue.getSummary(), issue.getDescription());
		// 2. get the response with the requirement id
		// 3. update the issue with the custom field "Supersede" set as the
		// requirement id
		if (null != requirementId) {
			updateIssue(issue, user, requirementId, errors);
		}
	}

	private void updateIssue(MutableIssue issue, ApplicationUser user, String requirementId,
			Collection<String> errors) {

		CustomField supersedeField = getSupersedeCustomField(getSupersedeCustomFieldType());
		issue.setCustomFieldValue(supersedeField, requirementId);
		Object customField = issue.getCustomFieldValue(supersedeField);
		log.debug("custom field of " + issue.getKey() + " set to " + customField);

		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		issueInputParameters.addCustomFieldValue(supersedeField.getId(), requirementId);
		IssueService.UpdateValidationResult updateRes = issueService.validateUpdate(user, issue.getId(),
				issueInputParameters);

		if (updateRes.getErrorCollection().hasAnyErrors()) {
			Map<String, String> errorsMap = updateRes.getErrorCollection().getErrors();
			for (String eid : errorsMap.keySet()) {
				errors.add(eid + ": " + errorsMap.get(eid));
			}
			log.error("cannot update issue for requirement " + requirementId);
		} else {
			IssueResult updated = issueService.update(user, updateRes);
			log.info("updated issue " + issue.getId() + " for requirement " + requirementId);

			Object updatedField = updated.getIssue().getCustomFieldValue(supersedeField);
			log.debug("updated custom field: ", updatedField);
		}
	}

	private void newRequirement(HttpServletRequest req, Collection<String> errors) {
		String issueKey = req.getParameter("issuekey");
		log.info("creating new requirement for " + issueKey);
		ApplicationUser user = getCurrentUser(req);
		IssueResult issueRes = issueService.getIssue(user, issueKey);
		if (issueRes.isValid()) {
			try {
				String sessionId = login();
				String xsrf = authenticate(sessionId);
				testCreateRequirement(sessionId, xsrf, issueRes.getIssue(), user, errors);
			} catch (Exception e) {
				log.error("login error: " + e);
			}
		} else {
			errors.add("invalid issue key " + issueKey);
		}
	}

	private String getCustomFieldId() {
		return "customfield_" + supersedeFieldId;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		Map<String, Object> context = Maps.newHashMap();

		// process request
		List<String> errors = new LinkedList<String>();
		if (!"".equals(req.getParameter(PARAM_ACTION)) && req.getParameter(PARAM_ACTION) != null) {
			// true = import clicked - false = attach clicked
			boolean isImport = "Import".equals(req.getParameter(PARAM_ACTION));
			// I retrieve Alert list anyway, both buttons require it
			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
			String issueID = "";
			if (isImport) {
				Alert a = fetchAlerts(req, list[0]).get(0);
				issueID = a.getId()+System.currentTimeMillis();
				newIssue(req, "Issue " + a.getId(), a.getDescription(), issueID, errors);
			}
			Alert a = null; 
			for (int i = 0; i < list.length; i++) {
				a = fetchAlerts(req, list[i]).get(0);
				if (isImport) {
					// attach file to the newly created issue
					errors.add("importing " + a.getId());
					attachToIssue(a, getIssues(req, issueID).get(0));

					// TODO: attach to an issue

				} else {
					// attach to an existing issue
					String[] issuesList = req.getParameter(PARAM_ISSUES_SELECTION_LIST).split(SEPARATOR);
					for (int j = 0; j < issuesList.length; j++) {
						errors.add("attaching " + a.getId());
						attachToIssue(a, getIssues(req, issuesList[j]).get(0));
					}
					// TODO: retrieve hidden input issue number
					// TODO: attach to that issue

				}
			}
			
			
			//FIXME: FIELDS BELOW HAVE TO BE REMOVED OR MOVED IN OTHER TABS IN FINAL VERSION
		} else if ("y".equals(req.getParameter("export"))) {
			errors.add("exporting " + req.getParameter("issuekey"));
			newRequirement(req, errors);
		} else if ("y".equals(req.getParameter("refreshAlerts"))) {
			// Reload just the alerts table template
			List<Alert> alerts = fetchAlerts(req);
			context.put("alerts", alerts);
			templateRenderer.render("/templates/supersede-man-alerts-table.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("refreshCompare"))) {
			// Reload just the comparison table template
			List<Difference> differences = compareIssues(req);
			context.put("differences", differences);
			templateRenderer.render("/templates/supersede-man-compare-table.vm", context, resp.getWriter());
			return;
		}
		// ---

		try {
			checkSupersedeField();
		} catch (Exception e) {
			log.error("checking custom supersede field: " + e);
		}
		// Render the list of issues (list.vm) if no params are passed in
		List<Difference> differences = compareIssues(req);

		List<Issue> issues = getIssues(req);
		List<Requirement> requirements = new LinkedList<Requirement>();
		getRequirements(req, requirements);
		List<Alert> alerts = fetchAlerts(req);

		context.put("alerts", alerts);
		context.put("issues", issues);
		context.put("requirements", requirements);
		context.put("differences", differences);
		context.put("errors", errors);
		context.put("separator", SEPARATOR);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		context.put("customFieldManager", customFieldManager);
		context.put("customFieldId", getCustomFieldId());
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render(MANAGER_BROWSER_TEMPLATE, context, resp.getWriter());
	}

	private void attachToIssue(Alert source, Issue target) {
		// If "Attach" button was clicked in alert table
		XMLFileGenerator xml = new XMLFileGenerator(source.getId(), new Date());
		File tmpFile = xml.generateXMLFile();
		if (tmpFile == null) {
			return;
		}

		CreateAttachmentParamsBean capb = new CreateAttachmentParamsBean.Builder(tmpFile, source.getId()+".xml", "application/xml",
				null, target).build();
		try {
			ComponentAccessor.getAttachmentManager().createAttachment(capb);
		} catch (AttachmentException e) {
			e.printStackTrace();
		}
	}

	private List<Difference> compareIssues(HttpServletRequest req) {
		List<Issue> JIRAissues = getIssues(req);
		List<Requirement> requirements = new LinkedList<Requirement>();
		List<Difference> differences = new LinkedList<Difference>();
		getRequirements(req, requirements, false);

		// ricerco gli ID jira nella lista requirements in modo da inserirli
		// come anomalie
		System.out.println("####### I RETRIEVED " + JIRAissues.size() + " JIRA Issues");
		System.out.println("####### I RETRIEVED " + requirements.size() + " SS Issues");
		log.error("####### I RETRIEVED " + JIRAissues.size() + " JIRA Issues");
		log.error("####### I RETRIEVED " + requirements.size() + " SS Issues");
		for (Issue i : JIRAissues) {
			for (Requirement r : requirements) {
				CustomField supersedeField = getSupersedeCustomField(getSupersedeCustomFieldType());
				String value = (String) i.getCustomFieldValue(supersedeField);
				log.error("VALUES " + String.valueOf(value) + " " + r.getId());
				if (String.valueOf(value).equals(r.getId())) {
					// Verifico la coerenza dei dati
					boolean equal = true;
					equal &= i.getDescription().equals(r.getDescription());
					if (!equal) {
						log.error("####### I RETRIEVED AN ISSUE THAT NEEDS TO BE SHOWN");
						Difference d = new Difference();
						d.setAnomalyType("DESCRIPTION");
						d.setId(r.getId());
						d.setJIRAValue(i.getDescription());
						d.setSSValue(r.getDescription());
						differences.add(d);
					}

				}
			}
		}
		return differences;
	}

	private List<Alert> fetchAlerts(HttpServletRequest req) {
		// retrieves a list of all alerts on SS
		return fetchAlerts(req, "");
	}

	private List<Alert> fetchAlerts(HttpServletRequest req, String alertId) {
		List<Alert> alerts = new LinkedList<Alert>();
		try {
			// retrieve the list of all alerts from the specified tenant
			String sessionId = login();
			if(alertId != null && !alertId.isEmpty()){
				alertId = "?id="+alertId;
			} else{
				alertId = "";
			}
			URL url = new URL(getUrl() + "/supersede-dm-app/alerts" + alertId);
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
				sb.append(output);
			}
			JSONArray jarr = new JSONArray(sb.toString());
			int l = jarr.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = jarr.getJSONObject(i);
				try {
					// We retrieve a list of alerts because there could be more
					// than one request per alert.
					// Every request could have different descriptions.
					List<Alert> a = parseJSONAsAlert(o);
					alerts.addAll(a);
				} catch (Exception e) {
					log.error("parsing ", o);
				}
			}

			conn.disconnect();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return alerts;
	}

	private List<Alert> parseJSONAsAlert(JSONObject o) {
		List<Alert> al = new LinkedList<Alert>();
		try {
			// Retrieval of requests linked to every alert
			JSONArray requests = o.getJSONArray("requests");
			for (int i = 0; i < requests.length(); i++) {
				// For every request, I create a custom Alert with significant
				// fields inside
				// It is a custom object created for JIRA, because I cannot use
				// linked projects or libraries.
				JSONObject r = requests.getJSONObject(i);
				Alert a = new Alert();
				a.setApplicationId(o.getString("applicationId"));
				a.setId(o.getString("id"));
				a.setTenant(o.getString("tenant"));
				Date d = new Date(/* o.getLong("timestamp") */);
				a.setTimestamp(d.toString());
				a.setDescription(r.getString("description"));
				al.add(a);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return al;
	}
}
