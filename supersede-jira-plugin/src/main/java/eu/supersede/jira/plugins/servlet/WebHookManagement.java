package eu.supersede.jira.plugins.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebHookManagement extends HttpServlet {

	private static final long serialVersionUID = -74390420067210080L;
	
	// NEEDS TO BE CALLED THIS WAY http://jira-supersede:2990/jira/plugins/servlet/webhook?webhook=y&issueKey=123&issueId=123

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("y".equals(req.getParameter("webhook"))) {
			String issueKey = req.getParameter("issueKey");
			String issueId = req.getParameter("issueId");

			System.out.println("##############################################");
			System.out.println("ISSUE EDITED FROM POST: " + issueKey + " " + issueId);
			System.out.println("##############################################");
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
