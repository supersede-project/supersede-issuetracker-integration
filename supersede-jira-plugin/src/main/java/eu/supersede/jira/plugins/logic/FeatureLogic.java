package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

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
	
	public String SendFeature(HttpServletRequest req) {
		int response = -1;
		String responseData = "";
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();
			URL url = new URL(loginLogic.getUrl() + "supersede-dm-app/processes/new");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

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
