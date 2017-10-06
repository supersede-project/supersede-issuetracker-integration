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

public class Difference {
	private String id;
	private String anomalyType;
	private String JIRAValue;
	private String SSValue;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAnomalyType() {
		return anomalyType;
	}
	public void setAnomalyType(String anomalyType) {
		this.anomalyType = anomalyType;
	}
	public String getJIRAValue() {
		return JIRAValue;
	}
	public void setJIRAValue(String jIRAValue) {
		JIRAValue = jIRAValue;
	}
	public String getSSValue() {
		return SSValue;
	}
	public void setSSValue(String SSValue) {
		this.SSValue = SSValue;
	}
	
}
