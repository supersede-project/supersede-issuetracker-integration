package eu.supersede.jira.plugins.servlet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
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
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
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
		Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(loginLogic.getCurrentUser());
		for (SearchRequest s : sList) {
			// s.getName()
		}
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
		ApplicationUser user = loginLogic.getCurrentUser();
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
			}
			resp.setContentType("text/html;charset=utf-8");
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
					ApplicationUser user = loginLogic.getCurrentUser();
					SearchRequest sr = ComponentAccessor.getComponentOfType(SearchRequestService.class).getFilter(new JiraServiceContextImpl(user), Long.valueOf(filter));

					String processSSID = processLogic.createProcess(req, id);

					// Get a list of issues from this query
					List<Issue> issueList = issueLogic.getIssuesFromFilter(req, sr.getQuery());
					StringBuilder issueRequirementsMap = new StringBuilder();

					HashMap<String, String> issueMap = new HashMap<String, String>();
					for (Issue i : issueList) {
						String restRequirementCreationResult = requirementLogic.createRequirement(processSSID, i);
						JSONObject jo = new JSONObject(restRequirementCreationResult);
						String requirementId = jo.getString("requirementId");
						// add issue to HashMap
						issueMap.put(i.getKey(), requirementId);
						// creating a String map "key###value,key2###value2.."
						issueRequirementsMap.append(i.getKey()).append(ProcessLogic.MAP_SEPARATOR).append(requirementId).append(",");

						// find issue links and send them to SS only if both
						// parts of issues are included in this process
						IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
						List<IssueLink> linkListO = issueLinkManager.getOutwardLinks(i.getId());
						List<IssueLink> linkListI = issueLinkManager.getInwardLinks(i.getId());
						// since in SS links go both ways, let's merge the lists
						List<IssueLink> mergedList = new LinkedList<IssueLink>();
						mergedList.addAll(linkListI);
						mergedList.addAll(linkListO);
						
						List<Long> requirementsToLink = new LinkedList<Long>();
						// For every link related to this issue, if target is in
						// process issues, add it as link
						
						//TODO: check if inwardsLink has inverted source/target data. In outwards we need target, don't know if inwards is the same
						boolean hasLinks = false;
						for (IssueLink il : mergedList) {
							if (!"Dependency".equals(il.getIssueLinkType().getName())) {
								continue;
							}
							Issue targetIssue = il.getDestinationObject();
							if (issueMap.containsKey(targetIssue.getKey())) {
								requirementsToLink.add(Long.parseLong(issueMap.get(targetIssue.getKey())));
								hasLinks = true;
							}
						}
						if (hasLinks) {
							requirementLogic.setRequirementLinks(processSSID, requirementId, requirementsToLink);
						}

					}

					// ProcessService added at last
					processService.add(description, processSSID, issueRequirementsMap.toString(), sr.getQuery().getQueryString(), "In progress");

				}
				res.sendRedirect(req.getContextPath() + "/plugins/servlet/supersede-prioritization");
			} else if ("rankingImport".equals(req.getParameter(PARAM_ACTION))) {
				String processId = req.getParameter("processId");
				SupersedeProcess sp = processService.getProcess(processId);
				JSONArray jarr = processLogic.getRankingJSONArray(processId);
				HashMap<String, String> irHashMap = processLogic.getIssueRequirementsHashMap(sp);

				JSONObject o = jarr.getJSONObject(0);
				JSONArray scores = o.getJSONArray("scores");
				int l = scores.length();
				scoresLoop: for (int i = 0; i < l; ++i) {
					String issueKey = irHashMap.get(scores.getJSONObject(i).getString("requirementId"));
					IssueManager issueManager = ComponentAccessor.getIssueManager();
					MutableIssue mIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
					if (mIssue == null) {
						// If no issue is found, skip this one but continue with
						// others
						continue scoresLoop;
					}
					// define priority
					// if l<5 , so priority = i (there are 5 priority levels)
					// if l > 5, priority = i%5;
					// Start to 1, setting priority to 0 leads to nothing
					int priorityValue = i + 1;
					if (l > 5) {
						double ratio = (double) l / 5;
						priorityValue = Math.min(5, (int) (i / ratio + 1));
					}
					mIssue.setPriorityId(String.valueOf(priorityValue));
					Date d = new Date();
					mIssue.setDescription(mIssue.getDescription() + " Priority set to " + priorityValue + " on " + d.toString());

					issueManager.updateIssue(loginLogic.getCurrentUser(), mIssue, EventDispatchOption.ISSUE_UPDATED, true);
				}

				sp.setLastRankingImportDate(new Date());
				sp.save();

			}
			doGet(req, res);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
