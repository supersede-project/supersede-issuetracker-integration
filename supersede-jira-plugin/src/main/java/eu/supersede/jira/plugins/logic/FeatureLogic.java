package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.JsonObject;

import eu.supersede.jira.plugins.activeobject.ReplanJiraLogin;

public class FeatureLogic {

	private static FeatureLogic logic;
	private static SimpleDateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static SimpleDateFormat RELEASE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	private FeatureLogic() {
	}

	public static FeatureLogic getInstance() {
		if (logic == null) {
			logic = new FeatureLogic();
		}
		return logic;
	}

	public String sendFeature(HttpServletRequest req, Issue i) {
		ArrayList<String> emptyList = new ArrayList<String>();
		return sendFeature(req, i, emptyList);
	}

	public String sendFeature(HttpServletRequest req, Issue i, ArrayList<String> dependencies) {
		int response = -1;
		String responseData = "";
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();

			// http://platform.supersede.eu:8280/replan/projects/<ReplanTenant>/features
			URL url = new URL(loginLogic.getReplanHost() + loginLogic.getReplanTenant() + "/features");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			JSONObject feature = new JSONObject();
			feature.put("id", i.getId());
			feature.put("code", i.getId());
			feature.put("name", i.getSummary());
			feature.put("description", i.getDescription());
			feature.put("effort", "100");
			feature.put("deadline", i.getDueDate() != null ? DUE_DATE_FORMAT.format(i.getDueDate()) : "");
			feature.put("priority", i.getPriority() != null ? i.getPriority().getId() : "0");
			feature.put("properties", new JSONArray());
			feature.put("required_skills", new JSONArray());
			feature.put("depends_on", new JSONArray());
			feature.put("jira_url", ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/" + i.getKey());
			JSONArray dependentFeature = new JSONArray();
			for (String dep : dependencies) {
				dependentFeature.put(dep);
			}
			feature.put("hard_dependencies", dependentFeature);
			feature.put("soft_dependencies", new JSONArray());

			JSONArray features = new JSONArray();
			features.put(feature);

			JSONObject container = new JSONObject();
			container.put("features", features);

			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(container.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			return "Issue " + i.getKey() + ": Feature successfully created";

		} catch (Exception e) {
			e.printStackTrace();
			return "Error on issue " + i.getKey() + ": the related feature has been already created";
		}

	}

	public String updateIssueFromFeature(HttpServletRequest req, Issue i, List<ReplanJiraLogin> usersList) {
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();

			int response = -1;
			String responseData = null;
			// http://platform.supersede.eu:8280/replan/projects/<ReplanTenant>/features/<id>
			URL url = new URL(loginLogic.getReplanHost() + loginLogic.getReplanTenant() + "/features?code=" + i.getId().toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			JSONObject feature = null;
			JSONArray array = new JSONArray(sb.toString());
			for (int j = 0; j < array.length(); j++) {
				// now that we have the getByCode API, we can get rid of the
				// "select-all-then-filter-by-code" pattern
				feature = array.getJSONObject(j);
			}

			if (feature == null) {
				return "No feature has the requested code";
			}

			// If there is "error" field, the issue does not exist
			boolean error = feature.has("error");
			if (error) {
				return "Feature not found. You have to export them before importing back";
			}

			String issueKey = i.getKey();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			MutableIssue mIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
			mIssue.setSummary(feature.getString("name"));
			// calculate time

			TimeZone tz = TimeZone.getTimeZone("Europe/Rome");
			Calendar cal = Calendar.getInstance(tz);
			
			// check if this feature is contained in a release
			String releaseDeadline = null;
			JSONObject release = feature.optJSONObject("release");
			if (release != null) {
				JSONObject assignedTo = release.getJSONObject("assigned_to");
				if (assignedTo != null && !"null".equals(assignedTo)) {
					cal.setTime(RELEASE_DATE_FORMAT.parse(assignedTo.getString("ends_at")));
					mIssue.setDueDate(new Timestamp(cal.getTimeInMillis()));

					for (ReplanJiraLogin login : usersList) {
						System.out.println(login.getReplanUsername().replace('_', ' ') + " AND " + assignedTo.getString("resource_name"));
						if (loginLogic.getReplanTenant().equals(login.getTenant()) && (login.getReplanUsername().replace('_', ' ')).equals(assignedTo.getString("resource_name"))) {
							ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(login.getJiraUsername());
							mIssue.setAssignee(user);
						}
					}
				}
			} else {
				String deadline = feature.getString("deadline");
				if (deadline != null && !"null".equals(deadline)) {
					cal.setTime(DUE_DATE_FORMAT.parse(deadline));
					mIssue.setDueDate(new Timestamp(cal.getTimeInMillis()));
				}
			}
			mIssue.setPriorityId(feature.getString("priority"));

			// get dependencies block from JSON
			JSONArray dependencies = feature.getJSONArray("depends_on");
			IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
			Long issueLinkTypeId = -1L;
			Collection<IssueLinkType> linkTypes = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class).getIssueLinkTypes(false);
			for (IssueLinkType ilt : linkTypes) {
				if ("Dependency".equals(ilt.getName())) {
					issueLinkTypeId = ilt.getId();
				}
			}

			if (issueLinkTypeId == -1L) {
				return "The type of the dependent issue link was not found on this system";
			}

			featureDependencyLoop: for (int j = 0; j < dependencies.length(); j++) {
				// now that we have the getByCode API, we can get rid of the
				// "select-all-then-filter-by-code" pattern
				JSONObject dep = dependencies.getJSONObject(j);
				// if the same dependency is still there, prevent insertion
				List<IssueLink> linkListO = issueLinkManager.getOutwardLinks(mIssue.getId());
				for (IssueLink il : linkListO /* mergedList */) {
					if (!"Dependency".equals(il.getIssueLinkType().getName())) {
						if (dep.getLong("code") == il.getDestinationId()) {
							continue featureDependencyLoop;
						}
					}
				}
				issueLinkManager.createIssueLink(mIssue.getId(), dep.getLong("code"), issueLinkTypeId, null, loginLogic.getCurrentUser());
			}

			// set assignee
			//

			issueManager.updateIssue(loginLogic.getCurrentUser(), mIssue, EventDispatchOption.ISSUE_UPDATED, true);
			return "Issue " + i.getKey() + " successfully updated";

		} catch (Exception e) {
			e.printStackTrace();
			return "Error on issue " + i.getKey() + " " + e.getMessage();
		}
	}

}
