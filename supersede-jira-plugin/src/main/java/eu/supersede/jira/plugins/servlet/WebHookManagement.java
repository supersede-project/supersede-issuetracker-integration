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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys.JiraIndexConfiguration.Issue;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.status.SimpleStatus;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.sun.mail.imap.protocol.FetchResponse;

import eu.supersede.jira.plugins.activeobject.ProcessService;
import eu.supersede.jira.plugins.activeobject.ProcessServiceImpl;
import eu.supersede.jira.plugins.activeobject.SupersedeLoginService;
import eu.supersede.jira.plugins.activeobject.SupersedeProcess;
import eu.supersede.jira.plugins.logic.AlertLogic;
import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.logic.ProcessLogic;
import eu.supersede.jira.plugins.logic.RequirementLogic;
import eu.supersede.jira.plugins.logic.SupersedeCustomFieldLogic;

public class WebHookManagement extends HttpServlet {

	private static final long serialVersionUID = -74390420067210080L;

	private TemplateRenderer templateRenderer;

	private LoginLogic loginLogic;

	private ProcessLogic processLogic;

	private RequirementLogic requirementLogic;

	private final ProcessService processService;

	List<String> errors = new LinkedList<String>();

	public WebHookManagement(IssueService issueService, ProjectService projectService, SearchService searchService, UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer templateRenderer,
			PluginSettingsFactory pluginSettingsFactory, CustomFieldManager customFieldManager, ProcessService processService, SupersedeLoginService ssLoginService) {
		this.templateRenderer = templateRenderer;

		loginLogic = LoginLogic.getInstance(ssLoginService);

		processLogic = ProcessLogic.getInstance();
		requirementLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		this.processService = checkNotNull(processService);

	}

	// NEEDS TO BE CALLED THIS WAY
	// http://jira-supersede:2990/jira/plugins/servlet/webhook?webhook=y&issueKey=123&issueId=123

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("y".equals(req.getParameter("webhook"))) {
			String issueKey = req.getParameter("issueKey");
			String issueId = req.getParameter("issueId");

			MutableIssue mi = ComponentAccessor.getIssueManager().getIssueByKeyIgnoreCase(issueKey);

			List<SupersedeProcess> processList = processService.getAllProcesses();
			ArrayList<String> requirements = new ArrayList<String>();
			
String name = mi.getStatusObject().getName();
System.out.println(name);

			// loop through all processes
			for (SupersedeProcess sp : processList) {
				if (!sp.getStatus().equals(ProcessLogic.STATUS_IN_PROGRESS)) {
					continue;
				}

				// retrieve map issue/requirement
				HashMap<String, String> map = processLogic.getIssueRequirementsHashMap(sp);

				// save requirements list - these are the ones to edit since

				for (Map.Entry<String, String> entry : map.entrySet()) {
					if (entry.getValue().equalsIgnoreCase(issueKey)) {
						requirements.add(entry.getKey());
					}
				}

				// Now i have the key list, I need to get every requirement and
				// edit it
				for (String s : requirements) {
					Requirement r = requirementLogic.getRequirement(s);
					if (r == null) {
						continue;
					}
					r.setName(mi.getSummary());
					r.setDescription(mi.getDescription());
					requirementLogic.editRequirement(r, mi.getKey());
				}
			}

			System.out.println("##############################################");
			System.out.println("ISSUE EDITED FROM POST: " + issueKey + " " + issueId);
			System.out.println("##############################################");
			return;
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("y".equals(req.getParameter("webhook"))) {
			String issueKey = req.getParameter("issueKey");
			String issueId = req.getParameter("issueId");

			System.out.println("##############################################");
			System.out.println("ISSUE EDITED FROM GET: " + issueKey + " " + issueId);
			System.out.println("##############################################");
			return;
		}
	}

}
