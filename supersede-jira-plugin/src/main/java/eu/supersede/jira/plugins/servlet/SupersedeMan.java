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

import eu.supersede.jira.plugins.logic.AlertLogic;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.RequirementLogic;

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

	private UserManager userManager;
	private TemplateRenderer templateRenderer;
	private final com.atlassian.jira.user.util.UserManager jiraUserManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final CustomFieldManager customFieldManager;

	private Long supersedeFieldId;

	private LoginLogic loginLogic;

	private IssueLogic issueLogic;

	private AlertLogic alertLogic;

	private RequirementLogic requirementLogic;

	public SupersedeMan(IssueService issueService, ProjectService projectService, SearchService searchService,
			UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager,
			TemplateRenderer templateRenderer, PluginSettingsFactory pluginSettingsFactory,
			CustomFieldManager customFieldManager) {
		this.userManager = userManager;
		this.templateRenderer = templateRenderer;
		this.jiraUserManager = jiraUserManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.customFieldManager = customFieldManager;
		loginLogic = LoginLogic.getInstance();
		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		alertLogic = AlertLogic.getInstance();
		requirementLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());
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

			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/requirement");
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			// conn.setRequestProperty("Authorization", getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
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
			issueLogic.updateIssue(issue, user, requirementId, errors,
					getSupersedeCustomField(getSupersedeCustomFieldType()));
		}
	}

	private void newRequirement(HttpServletRequest req, Collection<String> errors) {
		String issueKey = req.getParameter("issuekey");
		log.info("creating new requirement for " + issueKey);
		ApplicationUser user = loginLogic.getCurrentUser(req);
		IssueResult issueRes = issueLogic.getIssue(user, issueKey);
		if (issueRes.isValid()) {
			try {
				String sessionId = loginLogic.login();
				String xsrf = loginLogic.authenticate(sessionId);
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
				Alert a = alertLogic.fetchAlerts(req, list[0]).get(0);
				issueID = a.getId() + System.currentTimeMillis();
				issueLogic.newIssue(req, "Issue " + a.getId(), a.getDescription(), issueID, errors,
						getSupersedeCustomField(getSupersedeCustomFieldType()));
			}
			Alert a = null;
			for (int i = 0; i < list.length; i++) {
				a = alertLogic.fetchAlerts(req, list[i]).get(0);
				if (isImport) {
					// attach file to the newly created issue
					errors.add("importing " + a.getId());
					issueLogic.attachToIssue(a, issueLogic.getIssues(req, supersedeFieldId, issueID).get(0));

					// TODO: attach to an issue

				} else {
					// attach to an existing issue
					String[] issuesList = req.getParameter(PARAM_ISSUES_SELECTION_LIST).split(SEPARATOR);
					for (int j = 0; j < issuesList.length; j++) {
						errors.add("attaching " + a.getId());
						issueLogic.attachToIssue(a, issueLogic.getIssues(req, supersedeFieldId, issuesList[j]).get(0));
					}
					// TODO: retrieve hidden input issue number
					// TODO: attach to that issue

				}
			}

			// FIXME: FIELDS BELOW HAVE TO BE REMOVED OR MOVED IN OTHER TABS IN
			// FINAL VERSION
		} else if ("y".equals(req.getParameter("export"))) {
			errors.add("exporting " + req.getParameter("issuekey"));
			newRequirement(req, errors);
		} else if ("y".equals(req.getParameter("refreshAlerts"))) {
			// Reload just the alerts table template
			List<Alert> alerts = alertLogic.fetchAlerts(req);
			context.put("alerts", alerts);
			templateRenderer.render("/templates/supersede-man-alerts-table.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("refreshCompare"))) {
			// Reload just the comparison table template
			List<Difference> differences = issueLogic.compareIssues(req, supersedeFieldId,
					getSupersedeCustomField(getSupersedeCustomFieldType()));
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
		List<Difference> differences = issueLogic.compareIssues(req, supersedeFieldId,
				getSupersedeCustomField(getSupersedeCustomFieldType()));

		List<Issue> issues = issueLogic.getIssues(req, supersedeFieldId);
		List<Requirement> requirements = new LinkedList<Requirement>();
		requirementLogic.getRequirements(req, requirements, supersedeFieldId);
		List<Alert> alerts = alertLogic.fetchAlerts(req);

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

}
