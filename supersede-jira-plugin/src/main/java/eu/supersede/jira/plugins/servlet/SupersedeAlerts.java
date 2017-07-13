package eu.supersede.jira.plugins.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.ComponentManagerShutdownEvent;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import eu.supersede.jira.plugins.logic.AlertLogic;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.SupersedeCustomFieldLogic;
import webwork.action.ActionContext;

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
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager) {
		this.templateRenderer = templateRenderer;
		this.customFieldManager = customFieldManager;
		loginLogic = LoginLogic.getInstance();
		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		alertLogic = AlertLogic.getInstance();
		supersedeCustomFieldLogic = SupersedeCustomFieldLogic.getInstance(customFieldManager);

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

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		List<String> errors = new LinkedList<String>();
		if (!"".equals(req.getParameter("deleteAction")) && req.getParameter("deleteAction") != null) {
			// true = import clicked - false = attach clicked
			boolean isDelete = "Delete".equals(req.getParameter(PARAM_ACTION));
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

		// process request
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
				Alert a = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[0], "").get(0);
				issueID = a.getId() + System.currentTimeMillis();
				newIssue = issueLogic.newIssue(req, "Issue " + a.getId(), a.getDescription(), issueID, errors, supersedeCustomFieldLogic.getSupersedeCustomField(), project);
			}
			Alert a = null;
			for (int i = 0; i < list.length; i++) {
				a = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, list[i], "").get(0);
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

		} else if ("y".equals(req.getParameter("refreshAlerts"))) {
			List<Alert> alerts = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);
			List<Issue> issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId());
			context.put("issues", issues);
			context.put("alerts", alerts);
			context.put("date", new Date().toString());
			templateRenderer.render("/templates/content-supersede-alerts.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("searchAlerts"))) {
			String searchAlerts = req.getParameter(PARAM_SEARCH_ALERTS);
			List<Alert> alerts = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, "", searchAlerts);
			List<Issue> issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId());
			context.put("issues", issues);
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
			List<Issue> issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId(), req.getParameter(PARAM_SEARCH_ISSUES));
			context.put("issues", issues);
			context.put("date", new Date().toString());
			templateRenderer.render("/templates/attach-dialog-data.vm", context, resp.getWriter());
			return;
		} else if ("y".equals(req.getParameter("xmlAlert"))) {
			String xmlAlert = req.getParameter(PARAM_XML_ALERT);
			List<Alert> alerts = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic, xmlAlert, "");
			resp.setContentType("text/xml;charset=utf-8");
			context.put("alert", alerts.get(0));
			templateRenderer.render("/templates/xml-alert.vm", context, resp.getWriter());
			return;
		}
		List<Issue> issues = issueLogic.getIssues(req, supersedeCustomFieldLogic.getSupersedeFieldId());
		List<Alert> alerts = alertLogic.fetchAlerts(req, supersedeCustomFieldLogic.getSupersedeFieldId(), issueLogic);

		context.put("alerts", alerts);
		context.put("issues", issues);
		context.put("errors", errors);
		context.put("separator", SEPARATOR);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		resp.setContentType("text/html;charset=utf-8");
		// Pass in the list of issues as the context
		templateRenderer.render("/templates/logic-supersede-alerts.vm", context, resp.getWriter());
	}

}