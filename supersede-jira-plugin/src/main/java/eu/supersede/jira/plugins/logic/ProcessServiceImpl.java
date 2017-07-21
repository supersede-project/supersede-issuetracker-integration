package eu.supersede.jira.plugins.logic;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import eu.supersede.jira.plugins.servlet.SupersedeProcess;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

@Scanned
@Named
public class ProcessServiceImpl implements ProcessService{
	
    private final ActiveObjects ao;
	
	@Inject
	public ProcessServiceImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}

	@Override
	public SupersedeProcess add(String id, String desc, String query) {
		final SupersedeProcess process = ao.create(SupersedeProcess.class);
		process.setProcId(id);
		process.setDescription(desc);
		process.setQuery(query);
		process.save();
		return process;
	}

	@Override
	public SupersedeProcess add(String id, String desc, String query, String status) {
		SupersedeProcess process = add(id, desc, query);
//		final SupersedeProcess process = ao.create(SupersedeProcess.class);
//		process.setProcId(id);
//		process.setDescription(desc);
//		process.setQuery(query);
		process.setStatus(status);
		process.save();
		return process;
	}

	@Override
	public List<SupersedeProcess> getAllProcesses() {
		return newArrayList(ao.find(SupersedeProcess.class));
	}
	

}
