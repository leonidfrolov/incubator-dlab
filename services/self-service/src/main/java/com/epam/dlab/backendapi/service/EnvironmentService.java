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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.resources.dto.UserResourceInfo;

import java.util.List;
import java.util.Set;

public interface EnvironmentService {

	Set<String> getUserNames();

	List<UserDTO> getUsers();

	List<UserResourceInfo> getAllEnv();

	void stopAll();

	void stopEnvironment(String user);

	void stopEnvironmentWithServiceAccount(String user);

	void stopProjectEnvironment(String project);

	void stopEdge(String user);

	void stopExploratory(String user, String exploratoryName);

	void stopComputational(String user, String exploratoryName, String computationalName);

	void terminateAll();

	void terminateEnvironment(String user);

	void terminateExploratory(String user, String exploratoryName);

	void terminateComputational(String user, String exploratoryName, String computationalName);
}
