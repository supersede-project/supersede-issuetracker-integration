package eu.supersede.jira.plugins.servlet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import eu.supersede.jira.plugins.activeobject.ProcessService;
import eu.supersede.jira.plugins.activeobject.SupersedeProcess;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.ProcessLogic;
import eu.supersede.jira.plugins.logic.RequirementLogic;

public class SupersedePrioritization extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8918673397111520813L;

	private TemplateRenderer templateRenderer;

	private LoginLogic loginLogic;

	private IssueLogic issueLogic;

	private ProcessLogic processLogic;

	private RequirementLogic requirementLogic;

	private final ProcessService processService;

	private static final String PARAM_ACTION = "action";

	public SupersedePrioritization(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager, ProcessService processService) {
		this.templateRenderer = templateRenderer;
		processLogic = ProcessLogic.getInstance();
		loginLogic = LoginLogic.getInstance();
		requirementLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		this.processService = checkNotNull(processService);

		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());
	}

	public void getResult(HttpServletRequest req) {
		SharedEntitySearchParameters searchParams = new SharedEntitySearchParametersBuilder().setEntitySearchContext(SharedEntitySearchContext.USE).setName(null).setDescription(null).setFavourite(null).setSortColumn(SharedEntityColumn.NAME, true)
				.setUserName(null).setShareTypeParameter(null).setTextSearchMode(null).toSearchParameters();
		Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(loginLogic.getCurrentUser(req));
		for (SearchRequest s : sList) {
			// s.getName()
		}
		// SharedEntitySearchResult<SearchRequest> filtersResult =
		// cm.getSearchRequestService().
		// search(jsc, searchParams, 0, 50);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Map<String, Object> context = Maps.newHashMap();
		// process request
		List<String> errors = new LinkedList<String>();
		context.put("errors", errors);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
		context.put("projects", projects);
		ApplicationUser user = loginLogic.getCurrentUser(req);
		if (user != null) {
			Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(user);
			context.put("filters", sList);
			if ("y".equals(req.getParameter("loadIssues"))) {
				String filter = req.getParameter("filter");
				SearchRequest sr = ComponentAccessor.getComponentOfType(SearchRequestService.class).getFilter(new JiraServiceContextImpl(user), Long.valueOf(filter));
				context.put("issues", issueLogic.getIssuesFromFilter(req, sr.getQuery()));
				context.put("filter", sr);
				List<SupersedeProcess> processes = processService.getAllProcesses();
				processService.updateAllProcessesStatus(processes);
				context.put("processes", processes);
				templateRenderer.render("/templates/prioritization-export-data.vm", context, resp.getWriter());
				return;
				// sr.
				// nella issue logic caricare le issue collegate al filtro
			}
			// else if("y".equals(req.getParameter("createProcess"))){
			// String filter = req.getParameter("filter");
			// SearchRequest sr =
			// ComponentAccessor.getComponentOfType(SearchRequestService.class)
			// .getFilter(new JiraServiceContextImpl(user),
			// Long.valueOf(filter));
			// List<Issue> issues = issueLogic.getIssuesFromFilter(req,
			// sr.getQuery());
			//
			//
			// }
			resp.setContentType("text/html;charset=utf-8");
			// Pass in the list of issues as the context

			templateRenderer.render("/templates/logic-supersede-prioritization.vm", context, resp.getWriter());
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		try {
			// This must create a process on SS and "on JIRA" at the same time
			if ("CreateProc".equals(req.getParameter(PARAM_ACTION))) {
				String id = req.getParameter("procId");
				String description = req.getParameter("procDesc");
				String filter = req.getParameter("procFilter");
				if (filter != null && !filter.isEmpty()) {
					ApplicationUser user = loginLogic.getCurrentUser(req);
					SearchRequest sr = ComponentAccessor.getComponentOfType(SearchRequestService.class).getFilter(new JiraServiceContextImpl(user), Long.valueOf(filter));

					String processSSID = processLogic.createProcess(req, id);

					// Get a list of issues from this query
					List<Issue> issueList = issueLogic.getIssuesFromFilter(req, sr.getQuery());
					StringBuilder issueRequirementsMap = new StringBuilder();

					for (Issue i : issueList) {
						// creating a String map "key###value,key2###value2.."
						// and
						// so on
						String restResult = requirementLogic.createRequirement(processSSID, i);

						JSONObject jo = new JSONObject(restResult);
						String requirementId = jo.getString("requirementId");
						issueRequirementsMap.append(i.getKey()).append("###").append(requirementId).append(",");

					}

					// Create a requirement from

					// ProcessService added at last
					processService.add(description, processSSID, issueRequirementsMap.toString(), sr.getQuery().getQueryString(), "In progress");

				}

				// Create a Process and save the id provided by Supersede

				// Save a map of issues and Requirements (since an Issue can be
				// inserted in a Process even though the same issue could be
				// involved in another one)

				res.sendRedirect(req.getContextPath() + "/plugins/servlet/supersede-prioritization");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
