package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.Gson;

import eu.supersede.jira.plugins.servlet.Requirement;

public class RequirementLogic {

	private static RequirementLogic logic;

	private LoginLogic loginLogic;

	private IssueService issueService;

	private ProjectService projectService;

	private SearchService searchService;

	private static final Logger log = LoggerFactory.getLogger(RequirementLogic.class);

	private RequirementLogic(IssueService issueService, ProjectService projectService, SearchService searchService) {
		loginLogic = LoginLogic.getInstance();
	}

	public static RequirementLogic getInstance(IssueService issueService, ProjectService projectService, SearchService searchService) {
		if (logic == null) {
			logic = new RequirementLogic(issueService, projectService, searchService);
		}
		return logic;
	}

	private void fetchRequirements(String sessionId, Collection<Requirement> requirements) {
		try {

			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/requirement");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			log.debug("connection code " + conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				// System.out.println(output);
				sb.append(output);
			}
			JSONArray jarr = new JSONArray(sb.toString());
			// log.debug(jarr.toString(2));

			int l = jarr.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = jarr.getJSONObject(i);
				try {
					Requirement r = new Requirement(o);
					requirements.add(r);
				} catch (JSONException e) {
					log.error("parsing ", o);
				}
			}

			conn.disconnect();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Remove all the requirements which are already mapped as issues
	 * 
	 * @param user
	 * @param requirements
	 */
	private void filterRequirements(ApplicationUser user, Collection<Requirement> requirements, Long supersedeFieldId) {
		for (Iterator<Requirement> ir = requirements.iterator(); ir.hasNext();) {
			Requirement r = ir.next();
			Issue i = IssueLogic.getInstance(issueService, projectService, searchService).getIssueByRequirement(user, supersedeFieldId, r.getId());
			if (null != i) {
				ir.remove();
				log.debug("removed requirement " + r.getId() + " because already mapped to " + i.getKey());
			}
		}
	}

	public void getRequirements(HttpServletRequest req, Collection<Requirement> requirements, Long supersedeFieldId) {
		getRequirements(req, requirements, true, supersedeFieldId);
	}

	public void getRequirements(HttpServletRequest req, Collection<Requirement> requirements, boolean filter, Long supersedeFieldId) {
		try {
			ApplicationUser user = loginLogic.getCurrentUser();
			String sessionId = loginLogic.login();
			fetchRequirements(sessionId, requirements);
			if (filter) {
				filterRequirements(user, requirements, supersedeFieldId);
			}
		} catch (Exception e) {
			log.error("login error : " + e);
			return;
		}
	}

	private Requirement fetchRequirement(String sessionId, String id) {
		try {

			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/requirement/" + id);
			// URL url = new URL("http://supersede.es.atos.net:8080/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			// conn.setRequestProperty("Authorization", getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			log.debug("connection code " + conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				// System.out.println(output);
				sb.append(output);
			}
			JSONObject job = new JSONObject(sb.toString());
			log.debug(job.toString(2));

			Requirement r = new Requirement(job);

			conn.disconnect();
			return r;
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

	private Requirement getRequirement(String id) {
		try {
			String sessionId = loginLogic.login();
			return fetchRequirement(sessionId, id);
		} catch (Exception e) {
			log.error("login error : " + e);
			return null;
		}
	}

	public String createRequirement(String processId, Issue i) {
		int response = -1;
		String responseData = "";
		try {
			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "supersede-dm-app/processes/requirements/new");
			String jiraIssueUrl = " " + ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/";
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			StringBuilder params = new StringBuilder("processId=").append(processId).append("&name=").append(i.getKey()).append("&description=").append(i.getDescription() + jiraIssueUrl + i.getKey());
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(params.length()));
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setRequestProperty("X-XSRF-TOKEN", loginLogic.authenticate(sessionId));
			conn.setDoOutput(true);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(params.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			return sb.toString();

		} catch (

		Exception e) {
			e.printStackTrace();
		}

		return "";

	}
	
	public String setRequirementLinks(String processId, String requirementId, List<String> requirementsToLink) {
		int response = -1;
		String responseData = "";
		try {
			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "supersede-dm-app/processes/requirements//dependencies/submit");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			String listJson = new Gson().toJson(requirementsToLink);
			StringBuilder params = new StringBuilder("processId=").append(processId).append("&requirementId=").append(requirementId).append("&requirements=").append(listJson);
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(params.length()));
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setRequestProperty("X-XSRF-TOKEN", loginLogic.authenticate(sessionId));
			conn.setDoOutput(true);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(params.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			return sb.toString();

		} catch (

		Exception e) {
			e.printStackTrace();
		}

		return "";

	}

}
