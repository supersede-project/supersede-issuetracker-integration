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

public class SupersedePrioritizationList extends HttpServlet {

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
	List<String> errors = new LinkedList<String>();

	public SupersedePrioritizationList(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
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
		Map<String, Object> context = Maps.newHashMap();
		// process request
		context.put("processListFlag", "list");
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
		context.put("projects", projects);
		ApplicationUser user = loginLogic.getCurrentUser();
		if (user != null) {
			List<SupersedeProcess> processes = processService.getAllProcesses();
			processService.updateAllProcessesStatus(processes);
			context.put("processes", processes);
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
			if ("rankingImport".equals(req.getParameter(PARAM_ACTION))) {
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
					
					errors.add("Ranking of process  " + processId + " correctly imported");
					
				}

				sp.setLastRankingImportDate(new Date());
				sp.save();

			} else if ("closeProject".equals(req.getParameter(PARAM_ACTION))) {
				String processId = req.getParameter("processId");
				SupersedeProcess sp = processService.getProcess(processId);
				int closeResponse = processLogic.closeProcess(sp.getSSProjectId());
				if (closeResponse == 200) {
					sp.setLastRankingImportDate(new Date());
					sp.save();
				}
				
				errors.add("Supersede Process " + processId + " correctly closed");
			}
			
			else if ("removeProject".equals(req.getParameter(PARAM_ACTION))) {
				String processId = req.getParameter("processId");
				SupersedeProcess sp = processService.getProcess(processId);
				int closeResponse = processLogic.deleteProcess(sp.getSSProjectId());
				
				boolean requirementResult = true;
				for(String key : processLogic.getIssueRequirementsHashMap(sp).keySet()) {
					requirementResult &= requirementLogic.deleteRequirement(key);
				}

				if(!requirementResult) {
					System.out.println("Requirement deletion gave an error");
				}
				
				if (closeResponse == HttpURLConnection.HTTP_OK) {
					sp.setLastRankingImportDate(new Date());
					sp.save();
				}
				
				errors.add("Supersede Process " + sp.getID() + " correctly added");
			}
			doGet(req, res);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
