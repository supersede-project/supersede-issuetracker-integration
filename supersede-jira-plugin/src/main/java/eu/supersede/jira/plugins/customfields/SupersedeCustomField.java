package eu.supersede.jira.plugins.customfields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;

public class SupersedeCustomField extends AbstractSingleFieldType<String> {
	private static final Logger log = LoggerFactory.getLogger(SupersedeCustomField.class);

	public SupersedeCustomField(CustomFieldValuePersister customFieldValuePersister,
			GenericConfigManager genericConfigManager) {
		super(customFieldValuePersister, genericConfigManager);
	}

	@Override
	public String getStringFromSingularObject(final String singularObject) {
		if (singularObject == null)
			return "";
		else
			return singularObject;
	}

	@Override
	public String getSingularObjectFromString(final String string) throws FieldValidationException {
		return string;
	}

	@Override
	protected PersistenceFieldType getDatabaseType() {
		return PersistenceFieldType.TYPE_LIMITED_TEXT;
	}

	@Override
	protected String getObjectFromDbValue(final Object databaseValue) throws FieldValidationException {
		return getSingularObjectFromString((String) databaseValue);
	}
	
	@Override
	protected Object getDbValueFromObject(final String customFieldObject)
	{
	    return getStringFromSingularObject(customFieldObject);
	}
}