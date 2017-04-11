package eu.supersede.jira.plugins.customfields;

import com.atlassian.jira.issue.customfields.searchers.TextSearcher;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.web.FieldVisibilityManager;

public class SupersedeFieldSearcher extends TextSearcher {

	public SupersedeFieldSearcher(FieldVisibilityManager fieldVisibilityManager,
            JqlOperandResolver jqlOperandResolver,
            CustomFieldInputHelper customFieldInputHelper) {
        super(fieldVisibilityManager, jqlOperandResolver, customFieldInputHelper);
	}
}