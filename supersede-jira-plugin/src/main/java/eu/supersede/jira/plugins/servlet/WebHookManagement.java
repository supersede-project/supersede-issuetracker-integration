package eu.supersede.jira.plugins.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

import eu.supersede.jira.plugins.logic.IssueLogic;
import eu.supersede.jira.plugins.logic.LoginLogic;

public class WebHookManagement extends HttpServlet {

	private static final long serialVersionUID = -74390420067210080L;

	// NEEDS TO BE CALLED THIS WAY
	// http://jira-supersede:2990/jira/plugins/servlet/webhook?webhook=y&issueKey=123&issueId=123

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("y".equals(req.getParameter("webhook"))) {
			String issueKey = req.getParameter("issueKey");
			String issueId = req.getParameter("issueId");

			System.out.println("##############################################");
			System.out.println("ISSUE EDITED FROM POST: " + issueKey + " " + issueId);
			System.out.println("##############################################");
			
			Issue i = ComponentAccessor.getIssueManager().getIssueByKeyIgnoreCase(issueKey);
			if(i == null) {
				return;
			}

			URL url = new URL(
					"http://localhost:8080/alerts/add?key=" + i.getKey() + "&name=" + i.getSummary() + "&desc=" + i.getDescription());
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
			System.out.println(sb.toString());
			return;
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("y".equals(req.getParameter("webhook"))) {
			String issueKey = req.getParameter("issueKey");
			String issueId = req.getParameter("issueId");

			System.out.println("##############################################");
			System.out.println("ISSUE EDITED FROM GET: " + issueKey + " " + issueId);
			System.out.println("##############################################");
			return;
		}
	}

}
