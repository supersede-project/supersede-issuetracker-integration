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

package eu.supersede.jira.plugins.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;

public class SupersedeCustomFieldLogic {

	private static final Logger log = LoggerFactory.getLogger(SupersedeCustomFieldLogic.class);

	private static SupersedeCustomFieldLogic logic;

	private final static String SUPERSEDE_FIELD_NAME = "Supersede", SUPERSEDE_FIELD_TYPE = "eu.supersede.jira.plugins.supersede-jira-plugin:supersede-custom-field", CUSTOM_FIELD_SEARCHER = "com.atlassian.jira.plugin.system.customfieldtypes:textsearcher";
	private final CustomFieldManager customFieldManager;
	private Long supersedeFieldId;

	private SupersedeCustomFieldLogic(CustomFieldManager customFieldManager) {
		this.customFieldManager = customFieldManager;
	}
	
	public Long getSupersedeFieldId() {
		return supersedeFieldId;
	}

	public static SupersedeCustomFieldLogic getInstance(CustomFieldManager customFieldManager) {
		if (logic == null) {
			logic = new SupersedeCustomFieldLogic(customFieldManager);
		}
		return logic;
	}

	public String getCustomFieldId() {
		return "customfield_" + supersedeFieldId;
	}

	public void checkSupersedeField() throws Exception {
		CustomField supersedeField = getSupersedeCustomField();
		if (null == supersedeField) {
			CustomFieldSearcher fieldSearcher = customFieldManager.getCustomFieldSearcher(CUSTOM_FIELD_SEARCHER);
			List<JiraContextNode> contexts = new ArrayList<JiraContextNode>();
			contexts.add(GlobalIssueContext.getInstance());
			IssueTypeManager issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager.class);
			Collection<IssueType> issueTypes = issueTypeManager.getIssueTypes();
			// add supersede to all issue types
			List<IssueType> myIssueTypes = new LinkedList<IssueType>();
			for (IssueType it : issueTypes) {
				log.debug(it.getId() + " ", it.getName());
				myIssueTypes.add(it);
			}
			supersedeField = customFieldManager.createCustomField(SUPERSEDE_FIELD_NAME, "SUPERSEDE powered issue", getSupersedeCustomFieldType(), fieldSearcher, contexts, myIssueTypes);
			log.info("the supersede custom field has been installed to all the issue types");
		} else {
			log.info("the supersede custom field is already available");
		}
		supersedeFieldId = supersedeField.getIdAsLong();
		log.debug("supersede custom field id is " + supersedeFieldId);
	}

	public CustomFieldType getSupersedeCustomFieldType() {
		CustomFieldType supersedeFieldType = customFieldManager.getCustomFieldType(SUPERSEDE_FIELD_TYPE);
		if (null == supersedeFieldType) {
			log.error("no such custom field type found: " + SUPERSEDE_FIELD_TYPE);
			for (CustomFieldType t : customFieldManager.getCustomFieldTypes()) {
				log.debug(t.getName() + " " + t.getKey());
			}
			throw new NullPointerException("no " + SUPERSEDE_FIELD_TYPE + " custom field available");
		}
		return supersedeFieldType;
	}

	public CustomField getSupersedeCustomField() {
		CustomField supersedeField = null;
		Collection<CustomField> supersedeFields = customFieldManager.getCustomFieldObjectsByName(SUPERSEDE_FIELD_NAME);
		for (CustomField cf : supersedeFields) {
			if (cf.getCustomFieldType().equals(getSupersedeCustomFieldType())) {
				supersedeField = cf;
			}
		}
		return supersedeField;
	}
}
