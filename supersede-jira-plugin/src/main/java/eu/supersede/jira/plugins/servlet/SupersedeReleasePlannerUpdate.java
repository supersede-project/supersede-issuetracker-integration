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
import java.util.ArrayList;
import java.util.Collection;
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
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Maps;

import eu.supersede.jira.plugins.activeobject.ProcessService;
import eu.supersede.jira.plugins.activeobject.ReplanJiraLogin;
import eu.supersede.jira.plugins.activeobject.ReplanJiraLoginService;
import eu.supersede.jira.plugins.activeobject.SupersedeLoginService;
import eu.supersede.jira.plugins.logic.FeatureLogic;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;

public class SupersedeReleasePlannerUpdate extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8918673397111520813L;

	private TemplateRenderer templateRenderer;

	private LoginLogic loginLogic;

	private IssueLogic issueLogic;

	private ReplanJiraLoginService replanJiraLoginService;

	public SupersedeReleasePlannerUpdate(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager, ProcessService processService, SupersedeLoginService ssLoginService, ReplanJiraLoginService replanJiraLoginService) {
		this.templateRenderer = templateRenderer;
		loginLogic = LoginLogic.getInstance(ssLoginService);
		loginLogic.loadConfiguration(pluginSettingsFactory.createGlobalSettings());
		issueLogic = IssueLogic.getInstance(issueService, projectService, searchService);
		this.replanJiraLoginService = checkNotNull(replanJiraLoginService);
	}

	public void getResult(HttpServletRequest req) {
		SharedEntitySearchParameters searchParams = new SharedEntitySearchParametersBuilder().setEntitySearchContext(SharedEntitySearchContext.USE).setName(null).setDescription(null).setFavourite(null).setSortColumn(SharedEntityColumn.NAME, true)
				.setUserName(null).setShareTypeParameter(null).setTextSearchMode(null).toSearchParameters();
		Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(loginLogic.getCurrentUser());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Map<String, Object> context = Maps.newHashMap();
		// process request
		List<String> errors = new LinkedList<String>();
		context.put("errors", errors);
		context.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
		ApplicationUser user = loginLogic.getCurrentUser();
		if (user != null) {
			Collection<SearchRequest> sList = ComponentAccessor.getComponentOfType(SearchRequestService.class).getOwnedFilters(user);
			context.put("filters", sList);
			if ("y".equals(req.getParameter("features"))) {
				FeatureLogic featureLogic = FeatureLogic.getInstance();
				String filter = req.getParameter("procFilter");
				if (filter != null && !filter.isEmpty()) {
					SearchRequest sr = ComponentAccessor.getComponentOfType(SearchRequestService.class).getFilter(new JiraServiceContextImpl(user), Long.valueOf(filter));
					List<Issue> issueList = issueLogic.getIssuesFromFilter(req, sr.getQuery());
					List<ReplanJiraLogin> usersList = replanJiraLoginService.getAllLogins();
					for (Issue i : issueList) {
						errors.add(featureLogic.updateIssueFromFeature(req, i, usersList));

					}
				}
			}
		}
		context.put("updateFlag", "update");
		context.put("errors", errors);
		templateRenderer.render("/templates/logic-supersede-release-planner.vm", context, resp.getWriter());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
	}

}
