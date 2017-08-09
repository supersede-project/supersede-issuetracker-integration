package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import eu.supersede.jira.plugins.activeobject.SupersedeProcess;

public class ProcessLogic {

	private static ProcessLogic logic;

	private LoginLogic loginLogic;

	private static final Logger log = LoggerFactory.getLogger(AlertLogic.class);

	public final static String STATUS_DELETED = "Deleted";
	public final static String STATUS_IN_PROGRESS = "InProgress";
	public final static String STATUS_CLOSED = "Closed";
	public final static String STATUS_RETRY = "Retry";
	public final static String MAP_SEPARATOR = "###";

	private ProcessLogic() {
		loginLogic = LoginLogic.getInstance();
	}

	public static ProcessLogic getInstance() {
		if (logic == null) {
			logic = new ProcessLogic();
		}
		return logic;
	}

	public String createProcess(HttpServletRequest req, String processName) {
		int response = -1;
		String responseData = "";
		try {

			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "supersede-dm-app/processes/new");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			StringBuilder params = new StringBuilder("name=").append(processName);

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

	public String checkProcessStatus(String processId) {
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/processes/status?processId=" + processId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			conn.disconnect();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return STATUS_RETRY;
	}

	public int getRankingNumber(String processId) {
		return getRankingJSONArray(processId).length();
	}

	public JSONArray getRankingJSONArray(String processId) {
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/processes/rankings/list?processId=" + processId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			JSONArray jarr = new JSONArray(sb.toString());
			conn.disconnect();
			return jarr;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// If something happened, return 0. If it was e.g. a connection error,
		// it will restore that value on the first successful refresh
		return new JSONArray();
	}

	public HashMap<String, String> getIssueRequirementsHashMap(SupersedeProcess sp) {
		String[] issueRequirementsMap = sp.getIssuesRequirementsMap().split(",");
		HashMap<String, String> irHashMap = new HashMap<String, String>();
		// Create an hashmap of key-value
		// putting requirement as key
		for (int i = 0; i < issueRequirementsMap.length; i++) {
			String[] ir = issueRequirementsMap[i].split(MAP_SEPARATOR);
			irHashMap.put(ir[1], ir[0]);
		}
		return irHashMap;
		// Now I process just one ranking, but in the future there could be
		// more than one returned.

	}

	public int closeProcess(String processId) {
		int response = -1;
		String responseData = "";
		try {

			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "supersede-dm-app/processes/close");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			StringBuilder params = new StringBuilder("processId=").append(processId);

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

			if (response == 200) {
				return response;
			}

			return -1;

		} catch (

		Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

}
