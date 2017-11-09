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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
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

import org.jfree.util.Log;

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
import eu.supersede.jira.plugins.activeobject.SupersedeLoginService;
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
	
	Map<String, Object> context = Maps.newHashMap();

	private static final String PARAM_ACTION = "action";
	List<String> errors = new LinkedList<String>();

	public SupersedePrioritization(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager, ProcessService processService, SupersedeLoginService ssLoginService) {
		this.templateRenderer = templateRenderer;
		
		
		loginLogic = LoginLogic.getInstance(ssLoginService);
		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());
		
		processLogic = ProcessLogic.getInstance();
		requirementLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		this.processService = checkNotNull(processService);

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
		// process request
		
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
		context.put("projects", projects);
		ApplicationUser user = loginLogic.getCurrentUser();
		if (user != null) {
			Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(user);
			context.put("filters", sList);
			resp.setContentType("text/html;charset=utf-8");
			context.put("errors", errors);
			templateRenderer.render("/templates/logic-supersede-prioritization.vm", context, resp.getWriter());
		}
		context.put("errors", errors);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		try {
			// This must create a process on SS and "on JIRA" at the same time
			if ("CreateProc".equals(req.getParameter(PARAM_ACTION))) {
				String name = req.getParameter("procId");
				String description = req.getParameter("procDesc");
				String filter = req.getParameter("procFilter");
				if (filter != null && !filter.isEmpty()) {
					ApplicationUser user = loginLogic.getCurrentUser();
					SearchRequest sr = ComponentAccessor.getComponentOfType(SearchRequestService.class).getFilter(new JiraServiceContextImpl(user), Long.valueOf(filter));

					String processSSID = processLogic.createProcess(req, name);

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
						// If specs require INWARDS LINK management, swap
						// comments in following 2 lines
						List<IssueLink> linkListO = issueLinkManager.getOutwardLinks(i.getId());
						// List<IssueLink> linkListI =
						// issueLinkManager.getInwardLinks(i.getId());

						// If specs require BOTH WAYS link management
						// List<IssueLink> mergedList = new
						// LinkedList<IssueLink>();
						// mergedList.addAll(linkListI);
						// mergedList.addAll(linkListO);

						List<Long> requirementsToLink = new LinkedList<Long>();
						// For every link related to this issue, if target is in
						// process issues, add it as link

						// TODO: check if inwardsLink has inverted source/target
						// data. In outwards we need target, don't know if
						// inwards is the same
						boolean hasLinks = false;
						for (IssueLink il : linkListO /* mergedList */) {
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
					processService.add(name, description, processSSID, issueRequirementsMap.toString(), sr.getQuery().getQueryString(), "In progress");
					
					errors.add("Supersede Process " + processSSID + " correctly added");

				}
				context.put("errors", errors);
				res.sendRedirect(req.getContextPath() + "/plugins/servlet/supersede-prioritization-list");
			}
		
			doGet(req, res);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
