/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
