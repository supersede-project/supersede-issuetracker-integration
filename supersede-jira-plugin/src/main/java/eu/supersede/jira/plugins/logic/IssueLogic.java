package eu.supersede.jira.plugins.logic;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.query.Query;

import eu.supersede.jira.plugins.servlet.Alert;
import eu.supersede.jira.plugins.servlet.Difference;
import eu.supersede.jira.plugins.servlet.Requirement;
import eu.supersede.jira.plugins.servlet.XMLFileGenerator;

public class IssueLogic {

	private static IssueLogic logic;

	private LoginLogic loginLogic;

	private RequirementLogic requirementsLogic;

	private IssueService issueService;

	private ProjectService projectService;

	private SearchService searchService;

	private static final Logger log = LoggerFactory.getLogger(IssueLogic.class);

	private IssueLogic(IssueService issueService, ProjectService projectService, SearchService searchService) {
		loginLogic = LoginLogic.getInstance();
		requirementsLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		this.issueService = issueService;
		this.projectService = projectService;
		this.searchService = searchService;
	}

	public static IssueLogic getInstance(IssueService issueService, ProjectService projectService, SearchService searchService) {
		if (logic == null) {
			logic = new IssueLogic(issueService, projectService, searchService);
		}
		return logic;
	}

	public List<Issue> getIssues(HttpServletRequest req, Long supersedeFieldId) {
		return getIssues(req, supersedeFieldId, null);
	}

	public IssueResult getIssue(ApplicationUser user, String issueKey) {
		return issueService.getIssue(user, issueKey);
	}

	/**
	 * Retrieve the issues with a valid supersede field set
	 * 
	 * @param req
	 * @return
	 */
	public List<Issue> getIssues(HttpServletRequest req, Long supersedeFieldId, String id) {
		// User is required to carry out a search
		ApplicationUser user = loginLogic.getCurrentUser(req);

		// search issues

		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();

		// Build the basic Jql query
		jqlClauseBuilder.customField(supersedeFieldId).isNotEmpty().and().project(req.getParameter("projectField") != null ? req.getParameter("projectField") : loginLogic.getCurrentProject());
		if (id != null) {
			// if an ID is provided, use in in filter
			// ID MUST BE the beginnning of the string. You cannot put a
			// wildcard at the beginning of the search
			jqlClauseBuilder.and().sub().customField(supersedeFieldId).like(id + "*").or().field("key").eq(id).or().field("summary").like(id + "*").endsub();
		}
		Query query = jqlClauseBuilder.buildQuery();
		// A page filter is used to provide pagination. Let's use an unlimited
		// filter to
		// to bypass pagination.
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		return searchResults.getIssues();
	}

	public Issue getIssueByRequirement(ApplicationUser user, Long supersedeFieldId, String requirementId) {
		// search issues
		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();
		Query query = jqlClauseBuilder.customField(supersedeFieldId).like(requirementId).and().project(loginLogic.getCurrentProject()).buildQuery();
		log.debug(query.getQueryString());
		log.debug(query.getWhereClause().toString());
		// A page filter is used to provide pagination. Let's use an unlimited
		// filter to
		// to bypass pagination.
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		List<Issue> issues = searchResults.getIssues();
		if (0 == issues.size()) {
			log.debug("no issues found for requirement " + requirementId);
			return null;
		}
		if (1 < issues.size()) {
			log.warn("more issues mapped to the same requirement " + requirementId + ": returning the first found");
		}
		return issues.get(0);
	}

	private void newIssue(HttpServletRequest req, Collection<String> errors, CustomField supersedeField) {
		newIssue(req, req.getParameter("name"), req.getParameter("description"), req.getParameter("id"), errors, supersedeField, loginLogic.getCurrentProject().toUpperCase());
	}

	public IssueResult newIssue(HttpServletRequest req, String name, String description, String id, Collection<String> errors, CustomField supersedeField, String projectId) {
		IssueResult issue = null;
		ApplicationUser user = loginLogic.getCurrentUser(req);
		// Perform creation if the "new" param is passed in
		// First we need to validate the new issue being created
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		// We're only going to set the summary and description. The rest are
		// hard-coded to
		// simplify this tutorial.
		issueInputParameters.setSummary(name);
		issueInputParameters.setDescription(description);
		issueInputParameters.addCustomFieldValue(supersedeField.getId(), id);

		// We need to set the assignee, reporter, project, and issueType...
		// For assignee and reporter, we'll just use the currentUser
		// issueInputParameters.setAssigneeId(user.getName());
		issueInputParameters.setReporterId(user.getName());
		// We hard-code the project name to be the project with the TUTORIAL key
		Project project = projectService.getProjectByKey(user,
				/* loginLogic.getCurrentProject().toUpperCase() */ projectId).getProject();
		if (null == project) {
			errors.add("Cannot add issue for requirement " + id + ": no such project " + loginLogic.getCurrentProject());
		} else {
			issueInputParameters.setProjectId(project.getId());
			// We also hard-code the issueType to be a "bug" == 1
			issueInputParameters.setIssueTypeId(project.getIssueTypes().iterator().next().getId());
			// Perform the validation
			issueInputParameters.setSkipScreenCheck(true);
			IssueService.CreateValidationResult result = issueService.validateCreate(user, issueInputParameters);

			if (result.getErrorCollection().hasAnyErrors()) {
				Map<String, String> errorsMap = result.getErrorCollection().getErrors();
				for (String eid : errorsMap.keySet()) {
					errors.add(eid + ": " + errorsMap.get(eid));
				}
				log.error("cannot add issue for requirement " + id);
			} else {
				issue = issueService.create(user, result);
				log.info("added issue for requirement " + id);
			}
		}
		return issue;
	}

	public void updateIssue(MutableIssue issue, ApplicationUser user, String requirementId, Collection<String> errors, CustomField supersedeField) {

		issue.setCustomFieldValue(supersedeField, requirementId);
		Object customField = issue.getCustomFieldValue(supersedeField);
		log.debug("custom field of " + issue.getKey() + " set to " + customField);

		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		issueInputParameters.addCustomFieldValue(supersedeField.getId(), requirementId);
		IssueService.UpdateValidationResult updateRes = issueService.validateUpdate(user, issue.getId(), issueInputParameters);

		if (updateRes.getErrorCollection().hasAnyErrors()) {
			Map<String, String> errorsMap = updateRes.getErrorCollection().getErrors();
			for (String eid : errorsMap.keySet()) {
				errors.add(eid + ": " + errorsMap.get(eid));
			}
			log.error("cannot update issue for requirement " + requirementId);
		} else {
			IssueResult updated = issueService.update(user, updateRes);
			log.info("updated issue " + issue.getId() + " for requirement " + requirementId);

			Object updatedField = updated.getIssue().getCustomFieldValue(supersedeField);
			log.debug("updated custom field: ", updatedField);
		}
	}

	public void attachToIssue(Alert source, Issue target) {
		// If "Attach" button was clicked in alert table
		XMLFileGenerator xml = new XMLFileGenerator(source);
		File tmpFile = xml.generateXMLFile();
		if (tmpFile == null) {
			return;
		}

		CreateAttachmentParamsBean capb = new CreateAttachmentParamsBean.Builder(tmpFile, source.getId() + ".xml", "application/xml", null, target).build();
		try {
			ComponentAccessor.getAttachmentManager().createAttachment(capb);
		} catch (AttachmentException e) {
			e.printStackTrace();
		}
	}

	public List<Difference> compareIssues(HttpServletRequest req, Long supersedeFieldId, CustomField supersedeField) {
		List<Issue> JIRAissues = getIssues(req, supersedeFieldId);
		List<Requirement> requirements = new LinkedList<Requirement>();
		List<Difference> differences = new LinkedList<Difference>();
		requirementsLogic.getRequirements(req, requirements, false, supersedeFieldId);

		// ricerco gli ID jira nella lista requirements in modo da inserirli
		// come anomalie
		System.out.println("####### I RETRIEVED " + JIRAissues.size() + " JIRA Issues");
		System.out.println("####### I RETRIEVED " + requirements.size() + " SS Issues");
		log.error("####### I RETRIEVED " + JIRAissues.size() + " JIRA Issues");
		log.error("####### I RETRIEVED " + requirements.size() + " SS Issues");
		for (Issue i : JIRAissues) {
			for (Requirement r : requirements) {
				String value = (String) i.getCustomFieldValue(supersedeField);
				log.error("VALUES " + String.valueOf(value) + " " + r.getId());
				if (String.valueOf(value).equals(r.getId())) {
					// Verifico la coerenza dei dati
					boolean equal = true;
					equal &= i.getDescription().equals(r.getDescription());
					if (!equal) {
						log.error("####### I RETRIEVED AN ISSUE THAT NEEDS TO BE SHOWN");
						Difference d = new Difference();
						d.setAnomalyType("DESCRIPTION");
						d.setId(r.getId());
						d.setJIRAValue(i.getDescription());
						d.setSSValue(r.getDescription());
						differences.add(d);
					}

				}
			}
		}
		return differences;
	}

}
