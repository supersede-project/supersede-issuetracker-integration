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
import eu.supersede.jira.plugins.logic.ProcessLogic;
import eu.supersede.jira.plugins.servlet.Alert;
import net.java.ao.Query;

@Scanned
@Named
public class ProcessServiceImpl implements ProcessService {

	private final ActiveObjects ao;

	ProcessLogic processLogic = ProcessLogic.getInstance();

	@Inject
	public ProcessServiceImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}

	@Override
	public SupersedeProcess add(String name, String desc, String query) {
		final SupersedeProcess process = ao.create(SupersedeProcess.class);
		process.setName(name);
		process.setDescription(desc);
		process.setQuery(query);
		process.setSSProjectId("");
		process.save();
		return process;
	}

	@Override
	public SupersedeProcess add(String name, String desc, String query, String status) {
		SupersedeProcess process = add(name, desc, query);
		process.setStatus(status);
		process.save();
		return process;
	}

	@Override
	public SupersedeProcess add(String name, String desc, String processID, String issueRequirementsMap, String query, String status) {
		SupersedeProcess process = add(name, desc, query);
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
	public SupersedeProcess getProcess(String processId) {
		SupersedeProcess[] result = ao.find(SupersedeProcess.class, Query.select().where("SSPROJECT_ID LIKE ?", processId));
		if(result != null && result.length > 0) {
			return result[0];
		}
		return null;
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
			if (process.getSSProjectId() == null || "".equals(process.getSSProjectId())) {
				process.setStatus(ProcessLogic.STATUS_DELETED);
				process.save();
				continue;
			}
			process.setStatus(processLogic.checkProcessStatus(process.getSSProjectId()));

			if (ProcessLogic.STATUS_IN_PROGRESS.equals(process.getStatus())) {
				process.setRankings(processLogic.getRankingNumber(process.getSSProjectId()));
			}

			process.save();

		}
	}

}
