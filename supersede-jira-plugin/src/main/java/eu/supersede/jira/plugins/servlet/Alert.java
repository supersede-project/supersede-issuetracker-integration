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

import java.util.Date;

public class Alert {
	private String id;
	private String filteredId;
	private String base64Id;
	private String classification;
	private String applicationId;
	private String tenant;
	private Date date;
	private String timestamp;
	private String description;
	private String sameCluster;
	private String[] issues;
	private int sentiment;
	private int positive;
	private int negative;
	private int count;

	public Alert() {
		this.setId("---");
		this.setFilteredId("---");
		this.setClassification("---");
		this.setApplicationId("---");
		this.setTenant("---");
		this.setTimestamp("---");
		this.setDescription("---");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFilteredId() {
		return filteredId;
	}

	public void setFilteredId(String filteredId) {
		this.filteredId = filteredId;
	}

	public String getBase64Id() {
		return base64Id;
	}

	public void setBase64Id(String base64Id) {
		this.base64Id = base64Id;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSameCluster() {
		return sameCluster;
	}

	public void setSameCluster(String sameCluster) {
		this.sameCluster = sameCluster;
	}

	public String[] getIssues() {
		return issues;
	}

	public void setIssues(String[] issues) {
		this.issues = issues;
	}
	
	public int getSentiment() {
		return sentiment;
	}

	public void setSentiment(int sentiment) {
		this.sentiment = sentiment;
	}

	public int getPositive() {
		return positive;
	}

	public void setPositive(int positive) {
		this.positive = positive;
	}

	public int getNegative() {
		return negative;
	}

	public void setNegative(int negative) {
		this.negative = negative;
	}

	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
}
