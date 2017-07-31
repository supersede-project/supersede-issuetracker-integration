package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.project.Project;

public class ProcessLogic {

	private static ProcessLogic logic;

	private LoginLogic loginLogic;

	private static final Logger log = LoggerFactory.getLogger(AlertLogic.class);

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";

	}

}
