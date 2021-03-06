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

package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.RespectBinding;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
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

	public List<Alert> fetchAlerts(HttpServletRequest req, HttpServletResponse res, Long supersedeFieldId,
			IssueLogic il) {
		// retrieves a list of all alerts on SS
		return fetchAlerts(req, res, supersedeFieldId, il, "", "");
	}

	public List<Alert> fetchAlerts(HttpServletRequest req, HttpServletResponse res, Long supersedeFieldId,
			IssueLogic il, String alertId, String searchAlerts) {
		List<Alert> alerts = new LinkedList<Alert>();
		try {
			// retrieve the list of all alerts from the specified tenant
			String sessionId = loginLogic.login();
			String xsrf = loginLogic.authenticate(sessionId);
			HttpSession session = req.getSession();
			session.setAttribute("Cookie", "SESSION=" + sessionId + ";");

			String userRequestId = "";

			if (alertId != null && !alertId.isEmpty()) {
				String[] alertParts = alertId.split("URCD");
				if (alertParts.length > 1) {
					userRequestId = alertParts[1];
				}

				alertId = "?id=" + alertParts[0];
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
					// Every request could have sdifferent descriptions.
					List<Alert> a = parseJSONAsAlert(o, req, supersedeFieldId, il, searchAlerts);
					alerts.addAll(a);
				} catch (Exception e) {
					log.error("parsing ", o);
				}
			}

			if (userRequestId != null && !userRequestId.isEmpty()) {
				for (Alert al : alerts) {
					if (al.getId().contains(userRequestId)) {
						alerts.clear();
						alerts.add(al);
						return alerts;
					}
				}
			}

			conn.disconnect();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return alerts;
	}

	public List<Alert> fetchAlertsByBase64(HttpServletRequest req, HttpServletResponse res, Long supersedeFieldId,
			IssueLogic il, String base64Id, String searchAlerts) {
		List<Alert> result = new ArrayList<Alert>();
		List<Alert> alerts = fetchAlerts(req, res, supersedeFieldId, il);
		for (Alert a : alerts) {
			if (base64Id.equals(a.getBase64Id())) {
				result.add(a);
			}
		}

		return result;
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
		String responseData = "";
		try {
			// retrieve the list of all alerts from the specified tenant
			String sessionId = loginLogic.login();
			if (alertId != null && !alertId.isEmpty()) {
				// alertId = "?id="+(alertId.substring(0, alertId.indexOf("URCD")));
				alertId = alertId.substring(alertId.indexOf("URCD") + 4);
			} else {
				return false;
			}
			String xsrf = loginLogic.authenticate(sessionId);
			HttpSession session = req.getSession();
			session.setAttribute("Cookie", "SESSION=" + sessionId + ";");
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/alerts/userrequests/discard?id=" + alertId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("X-XSRF-TOKEN", xsrf);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			String txt = "";
			System.out.println(txt);
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			System.out.println(response);
			responseData = conn.getResponseMessage();

			conn.disconnect();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return response == HttpURLConnection.HTTP_OK;
	}

	private List<Alert> parseJSONAsAlert(JSONObject o, HttpServletRequest req, Long supersedeFieldId, IssueLogic il,
			String searchAlerts) {
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
				if (i > 0) {
					a.setSameCluster("X");
				} else {
					a.setSameCluster("A");
				}
				a.setApplicationId(o.getString("applicationId"));
				a.setId(o.getString("id") + "URCD" + r.getString("id"));
				a.setClassification(r.getString("classification"));
				a.setFilteredId("alert" + o.getString("id").replace('-', '_'));
				a.setBase64Id(Base64.encodeBase64URLSafeString(a.getId().getBytes()));
				a.setTenant(o.getString("tenant"));
				Date d = new Date(o.getLong("timestamp"));
				a.setDate(d);
				a.setTimestamp(d.toString());
				a.setDescription(r.getString("description"));
				a.setSentiment(r.getInt("overallSentiment"));
				a.setPositive(r.getInt("positiveSentiment"));
				a.setNegative(r.getInt("negativeSentiment"));
				Set<String> issues = getRelatedIssues(req, supersedeFieldId, il, a.getId());
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
					if (a.getId().toLowerCase().contains(searchAlerts)
							|| a.getDescription().toLowerCase().contains(searchAlerts)) {
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

	public List<String> checkAlertToAlertSimilarity(Alert a, List<Alert> alerts, HttpServletRequest req) {
		try {
			int response = -1;
			String responseData = "";
			double tolerance = 0.0;
			if (req.getParameter("clusterization-tolerance") != null) {
				switch (req.getParameter("clusterization-tolerance")) {
				case "low":
					tolerance = 0.3;
					break;
				case "med":
					tolerance = 0.5;
					break;
				case "high":
					tolerance = 0.66;
					break;
				default:
					tolerance = 0.0;
					break;
				}
			}

			String sessionId = loginLogic.login();
			String xsrf = loginLogic.authenticate(sessionId);
			HttpSession session = req.getSession();
			session.setAttribute("Cookie", "SESSION=" + sessionId + ";");
			URL url = new URL(loginLogic.getSimilarity());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("X-XSRF-TOKEN", xsrf);

			JSONObject similarity = new JSONObject();
			JSONObject feedback = new JSONObject();
			feedback.put("text", a.getDescription());
			similarity.put("k", Math.min(Integer.parseInt(req.getParameter("similarity-number")), alerts.size()));
			similarity.put("feedback", feedback);
			similarity.put("tenant", loginLogic.getCurrentProject());
			similarity.put("language", "en");

			JSONArray list = new JSONArray();
			int alertCounter = 1;
			for (Alert i : alerts) {
				if (a.getId().equals(i.getId())) {
					continue;
				}
				JSONObject singleAlert = new JSONObject();
				singleAlert.put("_id", alertCounter++);
				singleAlert.put("title", i.getFilteredId());
				singleAlert.put("description", i.getDescription());

				list.put(singleAlert);
			}

			similarity.put("requirements", list);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(similarity.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			System.out.println(sb.toString());
			JSONArray result = new JSONArray(sb.toString());
			List<String> similarityList = new ArrayList<String>();
			int l = result.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = result.getJSONObject(i);

				for (int j = 0; j < list.length(); j++) {
					JSONObject al = list.getJSONObject(j);
					if (al.getString("_id").equals(o.getString("id")) && o.getDouble("score") >= tolerance) {
						similarityList.add(al.getString("description"));
						break;
					}
				}

			}

			return similarityList;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public List<Alert> checkAlertToAlertClusterization(List<Alert> alerts, HttpServletRequest req) {
		// Inizio con una similarity normale col primo alert selezionato

		List<Alert> similarityList = new ArrayList<Alert>();

		while (alerts.size() > 0) {
			List<String> similarities = new ArrayList<String>();
			// rimuovo il primo alert dalla lista per analizzarlo, e lo aggiungo in testa al
			// risultato
			Alert a = alerts.remove(0);
			similarityList.add(a);
			similarities = checkAlertToAlertSimilarity(a, alerts, req);

			if (similarities == null) {
				continue;
			}
			// Rimuovo tutte le similarities dall'array alert presente
			for (String s : similarities) {
				for (Alert al : alerts) {
					if (al.getDescription().equals(s)) {
						similarityList.add(alerts.remove(alerts.indexOf(al)));
						break;
					}
				}
			}
			similarityList.add(new Alert());
		}

		return similarityList;
	}
}
