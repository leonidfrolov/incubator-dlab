/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.dropwizard.listeners;

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.cloud.CloudProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.lang.String.format;


@Slf4j
public class MongoStartupListener implements ServerLifecycleListener {

	private static final String ROLES_FILE_FORMAT = "/mongo/%s/mongo_roles.json";
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private final UserRoleDao userRoleDao;
	private final SelfServiceApplicationConfiguration configuration;
	private final SettingsDAO settingsDAO;

	@Inject
	public MongoStartupListener(UserRoleDao userRoleDao,
								SelfServiceApplicationConfiguration configuration, SettingsDAO settingsDAO) {
		this.userRoleDao = userRoleDao;
		this.configuration = configuration;
		this.settingsDAO = settingsDAO;
	}

	@Override
	public void serverStarted(Server server) {
		settingsDAO.setServiceBaseName(configuration.getServiceBaseName());
		settingsDAO.setConfOsFamily(configuration.getOs());
		if (configuration.getCloudProvider() == CloudProvider.AZURE) {
			settingsDAO.setAzureSsnInstanceSize(configuration.getSsnInstanceSize());
		}
		if (userRoleDao.findAll().isEmpty()) {
			log.debug("Populating DLab roles into database");
			userRoleDao.insert(getRoles());
		} else {
			log.info("Roles already populated. Do nothing ...");
		}
	}

	private List<UserRoleDto> getRoles() {
		try (InputStream is = getClass().getResourceAsStream(format(ROLES_FILE_FORMAT,
				configuration.getCloudProvider().getName()))) {
			return MAPPER.readValue(is, new TypeReference<List<UserRoleDto>>() {
			});
		} catch (IOException e) {
			log.error("Can not marshall dlab roles due to: {}", e.getMessage());
			throw new IllegalStateException("Can not marshall dlab roles due to: " + e.getMessage());
		}
	}
}
