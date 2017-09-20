package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.JsonObject;

public class FeatureLogic {

	private static FeatureLogic logic;

	private FeatureLogic() {
	}

	public static FeatureLogic getInstance() {
		if (logic == null) {
			logic = new FeatureLogic();
		}
		return logic;
	}

	public String sendFeature(HttpServletRequest req, Issue i) {
		int response = -1;
		String responseData = "";
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();

			// http://platform.supersede.eu:8280/replan/projects/<ReplanTenant>/features
			URL url = new URL(loginLogic.getReplanHost() + loginLogic.getReplanTenant() + "/features");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			JSONObject feature = new JSONObject();
			feature.put("id", i.getId());
			feature.put("name", i.getSummary());
			feature.put("description", i.getDescription());
			feature.put("effort", "100");
			feature.put("deadline", "2017-12-31");
			feature.put("priority", i.getPriority() != null ? i.getPriority().getName() : "0");
			feature.put("properties", new JSONArray());
			feature.put("required_skills", new JSONArray());
			feature.put("depends_on", new JSONArray());
			feature.put("hard_dependencies", new JSONArray());
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
			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

}
