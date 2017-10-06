package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;

import eu.supersede.jira.plugins.servlet.Alert;

public class ReplanLogic {
	private static ReplanLogic logic;

	private static final Logger log = LoggerFactory.getLogger(ReplanLogic.class);

	private ReplanLogic() {
	}

	public static ReplanLogic getInstance() {
		if (logic == null) {
			logic = new ReplanLogic();
		}
		return logic;
	}

	public List<String> getReplanUsersByTenant() {
		LoginLogic loginLogic = LoginLogic.getInstance();
		try {
			URL url = new URL(loginLogic.getReplanHost() + loginLogic.getReplanTenant() + "/resources");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			List<String> users = new ArrayList<String>();
			JSONArray jarr = new JSONArray(sb.toString());
			int l = jarr.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = jarr.getJSONObject(i);
				try {
					users.add(o.getString("name"));
					System.out.println(o.getString("name"));
				} catch (Exception e) {
					log.error("parsing ", o);
				}
			}
			conn.disconnect();
			return users;
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return null;
	}

}
