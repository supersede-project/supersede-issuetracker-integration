package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SimilarityLogic {

	public static String getAlertSimilarity(String alertId) {
	try {
		URL url = new URL("http://localhost:8080/alerts/list?id=" + alertId);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
		conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
		conn.setRequestMethod("GET");
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
	
	return "Error";
	}
}
