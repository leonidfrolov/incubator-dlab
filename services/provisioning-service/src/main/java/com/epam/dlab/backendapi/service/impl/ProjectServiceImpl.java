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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.ProjectCallbackHandler;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.dto.project.ProjectActionDTO;
import com.epam.dlab.dto.project.ProjectCreateDTO;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectServiceImpl implements ProjectService {
	private static final String PROJECT_IMAGE = "docker.dlab-project";
	private static final String EDGE_IMAGE = "docker.dlab-edge";
	private static final String CALLBACK_URI = "/api/project/status";
	@Inject
	protected RESTService selfService;
	@Inject
	private ProvisioningServiceApplicationConfiguration configuration;
	@Inject
	private FolderListenerExecutor folderListenerExecutor;
	@Inject
	private ICommandExecutor commandExecutor;
	@Inject
	private CommandBuilder commandBuilder;

	@Override
	public String create(UserInfo userInfo, ProjectCreateDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.CREATE, dto.getName(), "project", PROJECT_IMAGE,
				dto.getEndpoint());
	}

	@Override
	public String terminate(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.TERMINATE, dto.getName(), "project", PROJECT_IMAGE,
				dto.getEndpoint());
	}

	@Override
	public String start(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.START, dto.getName(), "edge", EDGE_IMAGE, dto.getEndpoint());
	}

	@Override
	public String stop(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.STOP, dto.getName(), "edge", EDGE_IMAGE, dto.getEndpoint());
	}

	private String executeDocker(UserInfo userInfo, ResourceBaseDTO dto, DockerAction action, String projectName,
								 String resourceType, String image, String endpoint) {
		String uuid = DockerCommands.generateUUID();

		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new ProjectCallbackHandler(selfService, userInfo.getName(), uuid,
						action, CALLBACK_URI, projectName, getEdgeClass(), endpoint));

		RunDockerCommand runDockerCommand = new RunDockerCommand()
				.withInteractive()
				.withName(String.join("_", userInfo.getSimpleName(), projectName, resourceType, action.toString(),
						Long.toString(System.currentTimeMillis())))
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), resourceType)
				.withResource(resourceType)
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withImage(image)
				.withAction(action);

		try {
			commandExecutor.executeAsync(userInfo.getName(), uuid, commandBuilder.buildCommand(runDockerCommand, dto));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return uuid;
	}

	private <T> Class<T> getEdgeClass() {
		if (configuration.getCloudProvider() == CloudProvider.AWS) {
			return (Class<T>) EdgeInfoAws.class;
		} else if (configuration.getCloudProvider() == CloudProvider.AZURE) {
			return (Class<T>) EdgeInfoAzure.class;
		} else if (configuration.getCloudProvider() == CloudProvider.GCP) {
			return (Class<T>) EdgeInfoGcp.class;
		}
		throw new IllegalArgumentException();
	}
}
