package eu.supersede.jira.plugins.activeobject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

@Scanned
@Named
public class ProcessServiceImpl implements ProcessService {

	private final ActiveObjects ao;

	@Inject
	public ProcessServiceImpl(ActiveObjects ao) {
		 this.ao = checkNotNull(ao);
	}

	@Override
	public SupersedeProcess add(String desc, String query) {
		final SupersedeProcess process = ao.create(SupersedeProcess.class);
		process.setDescription(desc);
		process.setQuery(query);
		process.setSSProjectId(-1);
		process.save();
		return process;
	}

	@Override
	public SupersedeProcess add(String desc, String query, String status) {
		SupersedeProcess process = add(desc, query);
		// final SupersedeProcess process = ao.create(SupersedeProcess.class);
		// process.setProcId(id);
		// process.setDescription(desc);
		// process.setQuery(query);
		process.setStatus(status);
		process.save();
		return process;
	}

	@Override
	public List<SupersedeProcess> getAllProcesses() {
		return newArrayList(ao.find(SupersedeProcess.class));
	}

	@Override
	public void addSingleIssue(int id, String issue) {
		SupersedeProcess process = ao.get(SupersedeProcess.class, id);
		process.setIssues(process.getIssues() + issue);
		process.save();
	}

}
