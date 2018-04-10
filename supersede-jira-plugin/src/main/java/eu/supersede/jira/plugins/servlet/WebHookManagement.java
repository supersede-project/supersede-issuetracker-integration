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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
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

	public WebHookManagement(IssueService issueService, ProjectService projectService, SearchService searchService,
			UserManager userManager, com.atlassian.jira.user.util.UserManager jiraUserManager,
			TemplateRenderer templateRenderer, PluginSettingsFactory pluginSettingsFactory,
			CustomFieldManager customFieldManager, ProcessService processService,
			SupersedeLoginService ssLoginService) {
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

			System.out.println(mi.getStatus().getName());

			if ("Done".equals(mi.getStatus().getName())) {

				// http://supersede.es.atos.net:8081/orchestrator/feedback/authenticate
				// superadmin/password

				String token = loginToOrchestrator();
				if (token.isEmpty()) {
					return;
				}

				String response = sendFeedback(token);

				return;
			}

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

	private String loginToOrchestrator() {

		int response = -1;
		String responseData = "";
		try {
			URL url = new URL("http://supersede.es.atos.net:8081/orchestrator/feedback/authenticate");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			JSONObject loginJSON = new JSONObject();
			loginJSON.put("name", "superadmin");
			loginJSON.put("password", "password");

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(loginJSON.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			System.out.println(conn.getResponseMessage());
			System.out.println(response);
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			System.out.println(sb.toString());
			JSONObject result = new JSONObject(sb.toString());

			return result.getString("token");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private String sendFeedback(String token) {
		int response = -1;
		String responseData = "";
		try {
			URL url = new URL(
					"http://supersede.es.atos.net:8081/orchestrator/feedback/en/applications/305/configurations/100/info");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestProperty("Authorization", token);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			JSONObject loginJSON = new JSONObject();
			loginJSON.put("text", "Hi, your feedback has been evaluated and it is currently implemented!");

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(loginJSON.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			System.out.println(conn.getResponseMessage());
			System.out.println(response);
			responseData = conn.getResponseMessage();
			return responseData;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
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
