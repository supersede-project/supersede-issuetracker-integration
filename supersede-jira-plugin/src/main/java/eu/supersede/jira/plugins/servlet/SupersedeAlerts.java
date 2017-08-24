package eu.supersede.jira.plugins.servlet;

import java.io.IOException;
import java.util.Calendar;
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

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
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
		List<String> errors = new LinkedList<String>();
		if (!"".equals(req.getParameter("deleteAction")) && req.getParameter("deleteAction") != null) {
			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
			for (int i = 0; i < list.length; i++) {
				String alertId = list[i];
				boolean deleted = alertLogic.discardAlert(req, alertId);
				if (deleted) {
					errors.add("alertId " + alertId + " deleted");
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

		List<String> errors = new LinkedList<String>();
		if (!"".equals(req.getParameter(PARAM_ACTION)) && req.getParameter(PARAM_ACTION) != null) {
			// true = import clicked - false = attach clicked
			boolean isImport = "Import".equals(req.getParameter(PARAM_ACTION));
			// I retrieve Alert list anyway, both buttons require it
			String[] list = req.getParameter(PARAM_SELECTION_LIST).split(SEPARATOR);
			String issueID = "";
			IssueResult newIssue = null;
			if (isImport) {
				String project = req.getParameter("projectField");
				Alert a = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[0], "").get(0);
				issueID = a.getId() + System.currentTimeMillis();
				newIssue = issueLogic.newIssue(req, "Issue " + a.getId(), a.getDescription(), issueID, errors, supersedeCustomFieldLogic.getSupersedeCustomField(), project);
			}
			Alert a = null;
			for (int i = 0; i < list.length; i++) {
				a = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[i], "").get(0);
				if (isImport) {
					// attach file to the newly created issue
					errors.add(newIssue != null ? newIssue.getIssue().getKey() : "importing " + a.getId());// else
					issueLogic.attachToIssue(a, issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueID).get(0));
				} else {
					// attach to an existing issue
					String[] issuesList = req.getParameter(PARAM_ISSUES_SELECTION_LIST).split(SEPARATOR);
					for (int j = 0; j < issuesList.length; j++) {
						Issue issue = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issuesList[j]).get(0);
						errors.add(issue != null ? issue.getKey() : "attaching " + a.getId());
						issueLogic.attachToIssue(a, issue);
					}
				}
				if (req.getParameter("chkDeleteStatus") != null && "true".equals(req.getParameter("chkDeleteStatus"))) {
					if (alertLogic.discardAlert(req, a.getId())) {
						errors.add("alertId " + a.getId() + " deleted");
					}
				}
			}

			// reload issue list in order to update counter
			alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);
			context.put("alerts", alerts);

		} else if ("y".equals(req.getParameter("refreshAlerts"))) {
			context.put("date", new Date().toString());
			templateRenderer.render("/templates/content-supersede-alerts.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("searchAlerts"))) {
			String searchAlerts = req.getParameter(PARAM_SEARCH_ALERTS);
			alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, "", searchAlerts);
			context.put("alerts", alerts);
			context.put("date", new Date().toString());
			templateRenderer.render("/templates/content-supersede-alerts.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("refreshCompare"))) {
			// Reload just the alerts table template
			List<Difference> differences = issueLogic.compareIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), supersedeCustomFieldLogic.getSupersedeCustomField());
			context.put("differences", differences);
			context.put("date", new Date().toString());
			templateRenderer.render("/templates/content-supersede-man-compare-table.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("searchIssues"))) {
			issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), req.getParameter(PARAM_SEARCH_ISSUES));
			context.put("issues", issues);
			context.put("date", new Date().toString());
			templateRenderer.render("/templates/attach-dialog-data.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("xmlAlert"))) {
			String xmlAlert = req.getParameter(PARAM_XML_ALERT);
			alerts = alertLogic.fetchAlerts(req, resp, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, xmlAlert, "");
			resp.setContentType("text/xml;charset=utf-8");
			context.put("alert", alerts.get(0));
			templateRenderer.render("/templates/xml-alert.vm", context, resp.getWriter());
			return;
		}

		context.put("errors", errors);
		context.put("separator", SEPARATOR);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render("/templates/logic-supersede-alerts.vm", context, resp.getWriter());
	}

}