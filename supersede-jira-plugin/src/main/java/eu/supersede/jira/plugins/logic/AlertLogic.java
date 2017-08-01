package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;

import eu.supersede.jira.plugins.servlet.Alert;

public class AlertLogic {

	private static AlertLogic logic;

	private LoginLogic loginLogic;

	private static final Logger log = LoggerFactory.getLogger(AlertLogic.class);

	private AlertLogic() {
		loginLogic = LoginLogic.getInstance();
	}

	public static AlertLogic getInstance() {
		if (logic == null) {
			logic = new AlertLogic();
		}
		return logic;
	}

	public List<Alert> fetchAlerts(HttpServletRequest req, Long supersedeFieldId, IssueLogic il) {
		// retrieves a list of all alerts on SS
		return fetchAlerts(req, supersedeFieldId, il, "", "");
	}

	public List<Alert> fetchAlerts(HttpServletRequest req, Long supersedeFieldId, IssueLogic il, String alertId, String searchAlerts) {
		List<Alert> alerts = new LinkedList<Alert>();
		try {
			// retrieve the list of all alerts from the specified tenant
			String sessionId = loginLogic.login();
			if (alertId != null && !alertId.isEmpty()) {
				alertId = "?id=" + alertId;
			} else {
				alertId = "";
			}
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/alerts" + alertId);
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
				sb.append(output);
			}
			JSONArray jarr = new JSONArray(sb.toString());
			int l = jarr.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = jarr.getJSONObject(i);
				try {
					// We retrieve a list of alerts because there could be more
					// than one request per alert.
					// Every request could have different descriptions.
					List<Alert> a = parseJSONAsAlert(o, req, supersedeFieldId, il, searchAlerts);
					alerts.addAll(a);
				} catch (Exception e) {
					log.error("parsing ", o);
				}
			}

			conn.disconnect();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return alerts;
	}

	public Set<String> getRelatedIssues(HttpServletRequest req, Long supersedeFieldId, IssueLogic il, String alertId) {
		// IssueLogic il = null;//IssueLogic.getInstance(issueService,
		// projectService, searchService)
		List<Issue> issuesList = il.getAllIssues(req, supersedeFieldId);
		int c = 0;
		Set<String> issues = new HashSet<String>();
		for (Issue i : issuesList) {
			Collection<Attachment> attachments = i.getAttachments();
			attachements: for (Attachment a : attachments) {
				if (a.getFilename().substring(0, a.getFilename().length() - 4).equals(alertId)) {
					issues.add(i.getKey());
					break attachements;
				}
			}
		}
		return issues;
	}

	public boolean discardAlert(HttpServletRequest req, String alertId) {
		// List<Alert> alerts = new LinkedList<Alert>();
		int response = -1;
		try {
			// retrieve the list of all alerts from the specified tenant
			String sessionId = loginLogic.login();
			if (alertId != null && !alertId.isEmpty()) {
				// alertId = "?id="+alertId;
			} else {
				return false;
			}
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/alerts/discard/" + alertId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setRequestMethod("DELETE");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// conn.setRequestProperty("Content-Type", "application/json");
			// conn.setRequestProperty("Authorization",
			// loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setRequestProperty("X-XSRF-TOKEN", loginLogic.authenticate(sessionId));

			// OutputStreamWriter wr = new
			// OutputStreamWriter(conn.getOutputStream());
			// wr.write(alertId);
			// wr.flush();

			response = conn.getResponseCode();
			conn.getInputStream();

			// JSONObject req = new JSONObject();
			// req.put("name", name);
			// req.put("description", description);

			log.debug("alert " + alertId + "deleted");

			// BufferedReader br = new BufferedReader(new
			// InputStreamReader((conn.getInputStream())));
			//
			// String output;
			// StringBuffer sb = new StringBuffer();
			// while ((output = br.readLine()) != null) {
			// sb.append(output);
			// }
			// JSONArray jarr = new JSONArray(sb.toString());
			// int l = jarr.length();
			// for (int i = 0; i < l; ++i) {
			// JSONObject o = jarr.getJSONObject(i);
			// try {
			// // We retrieve a list of alerts because there could be more
			// // than one request per alert.
			// // Every request could have different descriptions.
			// List<Alert> a = parseJSONAsAlert(o);
			// alerts.addAll(a);
			// } catch (Exception e) {
			// log.error("parsing ", o);
			// }
			// }
			//
			conn.disconnect();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return response == HttpURLConnection.HTTP_OK;
	}

	private List<Alert> parseJSONAsAlert(JSONObject o, HttpServletRequest req, Long supersedeFieldId, IssueLogic il, String searchAlerts) {
		List<Alert> al = new LinkedList<Alert>();
		try {
			// Retrieval of requests linked to every alert
			JSONArray requests = o.getJSONArray("requests");
			for (int i = 0; i < requests.length(); i++) {
				// For every request, I create a custom Alert with significant
				// fields inside
				// It is a custom object created for JIRA, because I cannot use
				// linked projects or libraries.
				JSONObject r = requests.getJSONObject(i);
				Alert a = new Alert();
				a.setApplicationId(o.getString("applicationId"));
				a.setId(o.getString("id"));
				a.setFilteredId("alert" + o.getString("id").replace('-', '_'));
				a.setTenant(o.getString("tenant"));
				Date d = new Date(/* o.getLong("timestamp") */);
				a.setTimestamp(d.toString());
				a.setDescription(r.getString("description"));
				a.setSentiment(r.getInt("overallSentiment"));
				a.setPositive(r.getInt("positiveSentiment"));
				a.setNegative(r.getInt("negativeSentiment"));
				// TODO
				Set<String> issues = getRelatedIssues(req, supersedeFieldId, il, o.getString("id"));
				a.setCount(issues.size());
				String[] issuesArray = issues.toArray(new String[issues.size()]);
				Arrays.sort(issuesArray, new Comparator<String>() {

					@Override
					public int compare(String o1, String o2) {
						return o1.compareTo(o2);
					}
				});
				a.setIssues(issuesArray);
				if (searchAlerts != null && !searchAlerts.isEmpty()) {
					searchAlerts = searchAlerts.toLowerCase();
					if (a.getId().toLowerCase().contains(searchAlerts) || a.getDescription().toLowerCase().contains(searchAlerts)) {
						al.add(a);
					}
				} else
					al.add(a);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return al;
	}
}
