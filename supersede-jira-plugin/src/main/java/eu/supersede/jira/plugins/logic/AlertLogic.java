package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.AttachmentStore.AttachmentAdapter;
import com.atlassian.jira.util.io.InputStreamConsumer;
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

	public List<Alert> fetchAlerts(HttpServletRequest req) {
		// retrieves a list of all alerts on SS
		return fetchAlerts(req, "");
	}

	public List<Alert> fetchAlerts(HttpServletRequest req, String alertId) {
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
					List<Alert> a = parseJSONAsAlert(o);
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

	public int getAlertCount(HttpServletRequest req, Long supersedeFieldId, IssueLogic il, String alertCode) {
		// IssueLogic il = null;//IssueLogic.getInstance(issueService,
		// projectService, searchService)
		List<Issue> issuesList = il.getIssues(req, supersedeFieldId);
		int c = 0;
		for (Issue i : issuesList) {
			Collection<Attachment> attachments = i.getAttachments();
			for (Attachment a : attachments) {
				if (a.getFilename().substring(0, a.getFilename().length() - 4).equals(alertCode)) {
					c++;
				}
			}
		}
		return c;
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
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/alerts/discard");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			Map<String, String> arguments = new HashMap<>();
			arguments.put("alertId", alertId);
			StringJoiner sj = new StringJoiner("&");
			for (Map.Entry<String, String> entry : arguments.entrySet())
				sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
			byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
			int length = out.length;

			conn.setFixedLengthStreamingMode(length);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			conn.connect();
			try (OutputStream os = conn.getOutputStream()) {
				os.write(out);
			}
			response = conn.getResponseCode();

			System.out.println(response);

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

	private List<Alert> parseJSONAsAlert(JSONObject o) {
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
				a.setTenant(o.getString("tenant"));
				Date d = new Date(/* o.getLong("timestamp") */);
				a.setTimestamp(d.toString());
				a.setDescription(r.getString("description"));
				// TODO
				// a.setCount(getAlertCount(req, supersedeFieldId, il,
				// alertCode));
				al.add(a);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return al;
	}
}