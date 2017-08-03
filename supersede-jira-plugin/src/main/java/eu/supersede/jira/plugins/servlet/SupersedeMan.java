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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import eu.supersede.jira.plugins.logic.AlertLogic;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.RequirementLogic;
import eu.supersede.jira.plugins.logic.SupersedeCustomFieldLogic;

/**
 * 
 * @author matteo.pedrotti@deltainformatica.eu
 *
 */
public class SupersedeMan extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2352887307960761479L;

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
	// STATIC CUSTOM STRING AND FIELDS

	private static final String SEPARATOR = ":::";
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
	
	private SupersedeCustomFieldLogic supersedeCustomFieldLogic;

	public SupersedeMan(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager) {
		this.userManager = userManager;
		this.templateRenderer = templateRenderer;
		this.jiraUserManager = jiraUserManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.customFieldManager = customFieldManager;
		loginLogic = LoginLogic.getInstance();
		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		alertLogic = AlertLogic.getInstance();
		supersedeCustomFieldLogic = SupersedeCustomFieldLogic.getInstance(customFieldManager);
		requirementLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());
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

	private void testCreateRequirement(String sessionid, String xsrf, MutableIssue issue, ApplicationUser user, Collection<String> errors) {
		log.debug("creating requirement for issue " + issue.getId());
		// 1. send the request to supersede
		String requirementId = sendPostRequest(sessionid, xsrf, issue.getSummary(), issue.getDescription());
		// 2. get the response with the requirement id
		// 3. update the issue with the custom field "Supersede" set as the
		// requirement id
		if (null != requirementId) {
			issueLogic.updateIssue(issue, user, requirementId, errors, supersedeCustomFieldLogic.getSupersedeCustomField());
		}
	}

	private void newRequirement(HttpServletRequest req, Collection<String> errors) {
		String issueKey = req.getParameter("issuekey");
		log.info("creating new requirement for " + issueKey);
		ApplicationUser user = loginLogic.getCurrentUser();
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

	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

//		Map<String, Object> context = Maps.newHashMap();
//		try {
//			supersedeCustomFieldLogic.checkSupersedeField();
//		} catch (Exception e) {
//			log.error("checking custom supersede field: " + e);
//		}
//		// process request
//		List<String> errors = new LinkedList<String>();
//		if (!"".equals(req.getParameter(PARAM_ACTION)) && req.getParameter(PARAM_ACTION) != null) {
//			// true = import clicked - false = attach clicked
//			boolean isImport = "Import".equals(req.getParameter(PARAM_ACTION));
//			// I retrieve Alert list anyway, both buttons require it
//			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
//			String issueID = "";
//			if (isImport) {
//				Alert a = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[0]).get(0);
//				issueID = a.getId() + System.currentTimeMillis();
//				issueLogic.newIssue(req, "Issue " + a.getId(), a.getDescription(), issueID, errors, supersedeCustomFieldLogic.getSupersedeCustomField());
//			}
//			Alert a = null;
//			for (int i = 0; i < list.length; i++) {
//				a = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[i]).get(0);
//				if (isImport) {
//					// attach file to the newly created issue
//					errors.add("importing " + a.getId());
//					issueLogic.attachToIssue(a, issueLogic.getIssues(req, supersedeFieldId, issueID).get(0));
//
//
//				} else {
//					// attach to an existing issue
//					String[] issuesList = req.getParameter(PARAM_ISSUES_SELECTION_LIST).split(SEPARATOR);
//					for (int j = 0; j < issuesList.length; j++) {
//						errors.add("attaching " + a.getId());
//						issueLogic.attachToIssue(a, issueLogic.getIssues(req, supersedeFieldId, issuesList[j]).get(0));
//					}
//
//				}
//			}
//
//			// FINAL VERSION
//		} else if ("y".equals(req.getParameter("export"))) {
//			errors.add("exporting " + req.getParameter("issuekey"));
//			newRequirement(req, errors);
//		} else if ("y".equals(req.getParameter("openAlerts")) || "y".equals(req.getParameter("refreshAlerts"))) {
//			List<Alert> alerts = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);
//			List<Issue> issues = issueLogic.getIssues(req, supersedeFieldId);
//			context.put("customFieldManager", customFieldManager);
//			context.put("customFieldId", supersedeCustomFieldLogic.getCustomFieldId());
//			context.put("issues", issues);
//			context.put("alerts", alerts);
//			context.put("date", new Date().toString());
//			if (req.getParameter("openAlerts") != null && "y".equals(req.getParameter("openAlerts"))) {
//				templateRenderer.render("/templates/logic-supersede-man-alerts-table.vm", context, resp.getWriter());
//			} else {
//				templateRenderer.render("/templates/content-supersede-man-alerts-table.vm", context, resp.getWriter());
//			}
//			return;
//		} else if ("y".equals(req.getParameter("openCompare")) || "y".equals(req.getParameter("refreshCompare"))) {
//			// Reload just the alerts table template
//			List<Difference> differences = issueLogic.compareIssues(req, supersedeFieldId, supersedeCustomFieldLogic.getSupersedeCustomField());
//			context.put("differences", differences);
//			context.put("date", new Date().toString());
//			if (req.getParameter("openCompare") != null && "y".equals(req.getParameter("openCompare"))) {
//				templateRenderer.render("/templates/logic-supersede-man-compare-table.vm", context, resp.getWriter());
//			} else {
//				templateRenderer.render("/templates/content-supersede-man-compare-table.vm", context, resp.getWriter());
//			}
//			return;
//		}
//		// ---
//
//		// Render the list of issues (list.vm) if no params are passed in
//		List<Difference> differences = issueLogic.compareIssues(req, supersedeFieldId, supersedeCustomFieldLogic.getSupersedeCustomField());
//
//		List<Issue> issues = issueLogic.getIssues(req, supersedeFieldId);
//		List<Requirement> requirements = new LinkedList<Requirement>();
//		requirementLogic.getRequirements(req, requirements, supersedeFieldId);
//		List<Alert> alerts = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);
//
//		context.put("alerts", alerts);
//		context.put("issues", issues);
//		context.put("requirements", requirements);
//		context.put("differences", differences);
//		context.put("errors", errors);
//		context.put("separator", SEPARATOR);
//		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
//		context.put("customFieldManager", customFieldManager);
//		context.put("customFieldId", supersedeCustomFieldLogic.getCustomFieldId());
//		resp.setContentType("text/html;charset=utf-8");
//		// Pass in the list of issues as the context
//		templateRenderer.render(MANAGER_BROWSER_TEMPLATE, context, resp.getWriter());
	}

}
