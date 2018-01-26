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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import eu.supersede.jira.plugins.activeobject.SupersedeLoginService;
import eu.supersede.jira.plugins.logic.AlertLogic;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.SupersedeCustomFieldLogic;

public class SupersedeAlerts extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3631683077311743450L;

	private static final Logger log = LoggerFactory.getLogger(SupersedeAlerts.class);

	// STATIC CUSTOM STRING AND FIELDS

	private static final String SEPARATOR = ":::";
	private static final String PARAM_ACTION = "action";
	private static final String PARAM_SELECTION_LIST = "selectionList";
	private static final String PARAM_ISSUES_SELECTION_LIST = "issuesSelectionList";
	private static final String PARAM_SEARCH_ALERTS = "searchAlertsInput";
	private static final String PARAM_SEARCH_ISSUES = "searchIssuesInput";
	private static final String PARAM_XML_ALERT = "xmlAlertId";

	// END CUSTOM STRING AND FIELDS SECTION

	private TemplateRenderer templateRenderer;
	private final CustomFieldManager customFieldManager;

	private LoginLogic loginLogic;

	private IssueLogic issueLogic;

	private AlertLogic alertLogic;

	private SupersedeCustomFieldLogic supersedeCustomFieldLogic;

	List<String> errors = new LinkedList<String>();

	public SupersedeAlerts(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager, SupersedeLoginService ssLoginService) {
		this.templateRenderer = templateRenderer;
		this.customFieldManager = customFieldManager;

		loginLogic = LoginLogic.getInstance(ssLoginService);
		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());

		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		alertLogic = AlertLogic.getInstance();
		supersedeCustomFieldLogic = SupersedeCustomFieldLogic.getInstance(customFieldManager);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		errors = new LinkedList<String>();
		if (!"".equals(req.getParameter("deleteAction")) && req.getParameter("deleteAction") != null) {
			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
			for (int i = 0; i < list.length; i++) {
				String alertId = list[i];
				boolean deleted = alertLogic.discardAlert(req, alertId);
				if (deleted) {
					errors.add("Alert with ID " + alertId + "was successfully deleted");
					req.setAttribute("fromPost", true);
				}
				// int count = alertLogic.getAlertCount(req,
				// supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic,
				// alertId);

			}

			doGet(req, resp);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		errors = new LinkedList<String>();
		if (req.getAttribute("fromPost") != null) {
			errors = new LinkedList<String>();
			req.removeAttribute("fromPost");
		}

		if ("y".equals(req.getParameter("webhook"))) {
			String issueKey = "test";
			String issueId = "prova";

			System.out.println("##############################################");
			System.out.println("ISSUE EDITED: " + issueKey + " " + issueId);
			System.out.println("##############################################");
			return;
		}

		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();
			String xsrf = loginLogic.authenticate(sessionId);

			loginLogic.setSessionCookie(resp, sessionId);
			loginLogic.setXsrfCookie(resp, xsrf);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		Map<String, Object> context = Maps.newHashMap();
		try {
			supersedeCustomFieldLogic.checkSupersedeField();
		} catch (Exception e) {
			log.error("checking custom supersede field: " + e);
		}
		context.put("customFieldManager", customFieldManager);
		context.put("customFieldId", supersedeCustomFieldLogic.getSupersedeFieldId());
		// PROJECTS RETRIEVAL, NEEDED BY ANY APP
		List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
		context.put("projects", projects);

		// Project Field
		// If no project is specified (e.g. at first start), insert first
		// project in list
		context.put("projectField", req.getParameter("projectField") != null && !"".equals(req.getParameter("projectField")) ? req.getParameter("projectField") : projects.get(0).getKey());

		// process request
		List<Issue> issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId());
		List<Alert> alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);

		context.put("alerts", alerts);
		context.put("issues", issues);

		Collection<IssueType> issueTypes = issueLogic.getIssueTypesByProject(req.getParameter("projectField") != null && !"".equals(req.getParameter("projectField")) ? req.getParameter("projectField") : projects.get(0).getKey());
		context.put("types", issueTypes);
		context.put("defaultType", issueTypes.iterator().next().getId());
		context.put("date", new Date().toString());

		// select filters for similarity
		ApplicationUser user = loginLogic.getCurrentUser();
		if (user != null) {
			Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(user);
			context.put("filters", sList);
		}

		if (!"".equals(req.getParameter(PARAM_ACTION)) && req.getParameter(PARAM_ACTION) != null) {
			// true = import clicked - false = attach clicked
			boolean isImport = "Import".equals(req.getParameter(PARAM_ACTION));
			// I retrieve Alert list anyway, both buttons require it
			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
			String issueID = "";
			IssueResult newIssue = null;
			if (isImport) {
				String project = req.getParameter("projectField");
				String type = req.getParameter("issueType");
				Alert a = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[0], "").get(0);
				issueID = a.getId() + System.currentTimeMillis();
				newIssue = issueLogic.newIssue(req, "Issue " + a.getId(), a.getDescription(), issueID, errors, supersedeCustomFieldLogic.getSupersedeCustomField(), project, type);
			}
			if (isImport && newIssue == null) {
				errors.add("Cannot add issue");
				context.put("errors", errors);
				templateRenderer.render("/templates/content-supersede-alerts.vm", context, resp.getWriter());
				return;
			}
			Alert a = null;
			boolean firstLoop = false;
			for (int i = 0; i < list.length; i++) {
				a = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[i], "").get(0);
				if (isImport) {
					// attach file to the newly created issue
					if (!firstLoop) {
						errors.add(newIssue != null ? newIssue.getIssue().getKey() : "importing " + a.getId());// else
						firstLoop = true;
					}
					issueLogic.attachToIssue(a, issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueID).get(0));
					context.put("newIssue", "true");
				} else {
					// attach to an existing issue
					String[] issuesList = req.getParameter(PARAM_ISSUES_SELECTION_LIST).split(SEPARATOR);
					for (int j = 0; j < issuesList.length; j++) {
						Issue issue = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issuesList[j]).get(0);
						if (!firstLoop) {
							errors.add(issue != null ? issue.getKey() : "attaching " + a.getId());
							firstLoop = true;
						}
						issueLogic.attachToIssue(a, issue);
						context.put("attachedIssue", "true");
					}
				}
				if (req.getParameter("chkDeleteStatus") != null && "true".equals(req.getParameter("chkDeleteStatus"))) {
					if (alertLogic.discardAlert(req, a.getId())) {
						errors.add("Alert ID " + a.getId() + "was successfully deleted");
					}
				}
			}

			// reload issue list in order to update counter
			alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);
			context.put("alerts", alerts);

		} else if ("y".equals(req.getParameter("refreshAlerts"))) {
			templateRenderer.render("/templates/content-supersede-alerts.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("searchAlerts"))) {
			String searchAlerts = req.getParameter(PARAM_SEARCH_ALERTS);
			alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, "", searchAlerts);
			context.put("alerts", alerts);
			templateRenderer.render("/templates/content-supersede-alerts.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("searchIssues"))) {
			issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), req.getParameter(PARAM_SEARCH_ISSUES));
			context.put("issues", issues);
			templateRenderer.render("/templates/attach-dialog-data.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("xmlAlert"))) {
			String xmlAlert = req.getParameter(PARAM_XML_ALERT);
			alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, xmlAlert, "");
			resp.setContentType("text/xml;charset=utf-8");
			context.put("alert", alerts.get(0));
			templateRenderer.render("/templates/xml-alert.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("getIssueTypes"))) {
			issueTypes = issueLogic.getIssueTypesByProject(req.getParameter("projectField"));
			context.put("types", issueTypes);
			context.put("defaultType", issueTypes.iterator().next().getId());
			templateRenderer.render("/templates/content-supersede-alerts-issue-type.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("similarity"))) {
			String issueFilter = req.getParameter("issueFilter");
			System.out.println(issueFilter);
			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
			List<Issue> issuesList = null;
			if ("empty".equals(issueFilter)) {
				issuesList = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId());
			} else {
				SearchRequest sr = ComponentAccessor.getComponentOfType(SearchRequestService.class).getFilter(new JiraServiceContextImpl(user), Long.valueOf(issueFilter));
				// Get a list of issues from this query
				issuesList = issueLogic.getIssuesFromFilter(req, sr.getQuery());
			}
			ArrayList<String> similarities = new ArrayList<String>();
			for (int i = 0; i < list.length; i++) {
				Alert a = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[i], "").get(0);
				List<String> similarity = issueLogic.checkSimilarity(a, issuesList, req);
				similarities.add("Similarity for alert " + a.getId() + ": ");

				for (String s : similarity) {
					similarities.add(s);
				}
			}
			context.put("similarities", similarities);
			System.out.println("DONE");
		}

		issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId());
		context.put("issues", issues);
		context.put("errors", errors);
		context.put("separator", SEPARATOR);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render("/templates/logic-supersede-alerts.vm", context, resp.getWriter());
	}

}