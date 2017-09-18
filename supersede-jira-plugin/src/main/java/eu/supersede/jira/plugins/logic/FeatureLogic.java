package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.issue.Issue;

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

			// http://platform.supersede.eu:8280/replan/projects/<tenant>/features
			// TODO: force tenant to a 4 as soon as upc gives us access
			// URL url = new URL(loginLogic.getReplanHost() +
			// loginLogic.getCurrentProject() + "/features");
			// HttpURLConnection conn = (HttpURLConnection)
			// url.openConnection();
			// StringBuilder params = new
			// StringBuilder("id=").append(i.getId());
			// params.append("name=").append(i.getSummary());
			// params.append("description").append(i.getDescription());
			// params.append("effort").append(i.getEstimate());
			// params.append("deadline").append(i.getDueDate());
			// params.append("priority").append(i.getPriority().getId());
			// conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			// conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			// conn.setDoOutput(true);
			// conn.setRequestMethod("POST");
			// conn.setRequestProperty("Content-Type",
			// "application/x-www-form-urlencoded");
			// conn.setRequestProperty("Content-Length",
			// String.valueOf(params.length()));
			// conn.setRequestProperty("TenantId",
			// loginLogic.getCurrentProject());
			// conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			// conn.setRequestProperty("X-XSRF-TOKEN",
			// loginLogic.authenticate(sessionId));
			// conn.setDoOutput(true);
			// OutputStreamWriter outputStreamWriter = new
			// OutputStreamWriter(conn.getOutputStream());
			// outputStreamWriter.flush();
			//
			// response = conn.getResponseCode();
			// responseData = conn.getResponseMessage();
			//
			// BufferedReader br = new BufferedReader(new
			// InputStreamReader((conn.getInputStream())));
			// StringBuilder sb = new StringBuilder();
			// String output;
			// while ((output = br.readLine()) != null) {
			// sb.append(output);
			// }
			// return sb.toString();

			return "IT WORKS";

		} catch (

		Exception e) {
			e.printStackTrace();
		}

		return "";
	}

}
