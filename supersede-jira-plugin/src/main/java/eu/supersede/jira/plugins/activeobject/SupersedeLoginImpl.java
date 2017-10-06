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

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import net.java.ao.Query;

@Scanned
@Named
public class SupersedeLoginImpl implements SupersedeLoginService {

	private final ActiveObjects ao;

	@Inject
	public SupersedeLoginImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}

	@Override
	public SupersedeLogin add(String jiraUser, String ssUser, String ssPassword, String tenant) {
		final SupersedeLogin login = ao.create(SupersedeLogin.class);
		login.setJiraUser(jiraUser);
		login.setSSUser(ssUser);
		login.setSSPassword(ssPassword);
		login.setTenant(tenant);
		login.save();
		return login;
	}

	@Override
	public SupersedeLogin update(SupersedeLogin source, String jiraUser, String ssUser, String ssPassword, String tenant) {
		SupersedeLogin login = ao.get(SupersedeLogin.class, source.getID());
		if (login == null) {
			return null;
		}
		login.setJiraUser(jiraUser);
		login.setSSUser(ssUser);
		login.setSSPassword(ssPassword);
		login.setTenant(tenant);
		login.save();
		return login;
	}

	@Override
	public SupersedeLogin getLogin(String jiraUser) {
		SupersedeLogin[] result = ao.find(SupersedeLogin.class, Query.select().where("JIRA_USER LIKE ?", jiraUser));
		if (result != null && result.length > 0) {
			return result[0];
		}
		return null;
	}

}
