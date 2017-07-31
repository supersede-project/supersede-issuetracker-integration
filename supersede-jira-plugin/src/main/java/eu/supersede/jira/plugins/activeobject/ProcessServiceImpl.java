package eu.supersede.jira.plugins.activeobject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import eu.supersede.jira.plugins.logic.LoginLogic;
import eu.supersede.jira.plugins.servlet.Alert;

@Scanned
@Named
public class ProcessServiceImpl implements ProcessService {

	private final ActiveObjects ao;

	@Inject
	public ProcessServiceImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}

	@Override
	public SupersedeProcess add(String desc, String query) {
		final SupersedeProcess process = ao.create(SupersedeProcess.class);
		process.setDescription(desc);
		process.setQuery(query);
		process.setSSProjectId("");
		process.save();
		return process;
	}

	@Override
	public SupersedeProcess add(String desc, String query, String status) {
		SupersedeProcess process = add(desc, query);
		process.setStatus(status);
		process.save();
		return process;
	}

	@Override
	public SupersedeProcess add(String desc, String processID, String issueRequirementsMap, String query, String status) {
		SupersedeProcess process = add(desc, query);
		process.setSSProjectId(processID);
		process.setIssuesRequirementsMap(issueRequirementsMap);
		process.setStatus(status);
		process.save();
		return process;
	}

	@Override
	public List<SupersedeProcess> getAllProcesses() {
		return newArrayList(ao.find(SupersedeProcess.class));
	}

	@Override
	public void addSingleIssue(int id, String issue) {
		SupersedeProcess process = ao.get(SupersedeProcess.class, id);
		process.setIssues(process.getIssues() + issue);
		process.save();
	}

	@Override
	public void updateAllProcessesStatus(List<SupersedeProcess> processList) {
		for (SupersedeProcess process : processList) {
			try {
				if (process.getSSProjectId() == null || "".equals(process.getSSProjectId())) {
					process.setStatus("Deleted");
					process.save();
					continue;
				}
				LoginLogic loginLogic = LoginLogic.getInstance();
				String sessionId = loginLogic.login();
				URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/processes/status?processId=" + process.getSSProjectId());
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

				process.setStatus(sb.toString());
				process.save();

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
