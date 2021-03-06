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

import java.util.List;

import com.atlassian.activeobjects.tx.Transactional;

@Transactional
public interface ProcessService {

	SupersedeProcess add(String name, String desc, String query);

	SupersedeProcess add(String name, String desc, String query, String status);

	SupersedeProcess add(String name, String desc, String processID, String issueRequirementsMap, String query, String status);

	public SupersedeProcess add(String name, String desc, String processID, String issueRequirementsMap, String query, String status, String SSProcessLink);

	List<SupersedeProcess> getAllProcesses();

	SupersedeProcess getProcess(String processId);

	void updateAllProcessesStatus(List<SupersedeProcess> processList);

	void addSingleIssue(int id, String issue);

	void delete();

	void deleteJIRAProcess(String processId);

}
