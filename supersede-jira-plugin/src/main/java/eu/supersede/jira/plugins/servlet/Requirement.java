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

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Used as a container for all the SUPERSEDE Requirement or Feature data
 * @author matteo.pedrotti@deltainformatica.eu
 *
 */
public class Requirement {

	private String id;
	private String name;
	private String description;
	private int priority;
	private double effort;

	public Requirement() {
		id = "0";
		name = "name";
		description = "description";
		priority = 1;
		effort = 12.34d;
	}

	public Requirement(JSONObject o) throws JSONException {
		id = o.getString("requirementId");
		name = o.getString("name");
		description = o.getString("description");
		priority = -1;
		effort = -1d;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public double getEffort() {
		return effort;
	}

	public void setEffort(double effort) {
		this.effort = effort;
	}
}
