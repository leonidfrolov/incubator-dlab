/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.domain.ExploratoryLibCache;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.backendapi.resources.dto.SearchLibsFormDTO;
import com.epam.dlab.backendapi.service.LibraryService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/infrastructure_provision/exploratory_environment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class LibExploratoryResource {

    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ExploratoryLibDAO libraryDAO;

    @Inject
    private LibraryService libraryService;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /**
     * Returns the list of libraries groups for exploratory.
     *
     * @param userInfo        user info.
     * @param exploratoryName name of exploratory image.
     */
    @GET
    @Path("/lib_groups")
    public Iterable<String> getLibGroupList(@Auth UserInfo userInfo,
                                            @QueryParam("exploratory_name") @NotBlank String exploratoryName,
                                            @QueryParam("computational_name") String computationalName) {

        log.trace("Loading list of lib groups for user {} and exploratory {}, computational {}", userInfo.getName(),
                exploratoryName, computationalName);
        try {
            if (StringUtils.isEmpty(computationalName)) {
                UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), exploratoryName);
                return ExploratoryLibCache.getCache().getLibGroupList(userInfo, userInstance);
            } else {
                UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(),
                        exploratoryName, computationalName);

                userInstance.setResources(userInstance.getResources().stream()
                        .filter(e -> e.getComputationalName().equals(computationalName))
                        .collect(Collectors.toList()));

                return ExploratoryLibCache.getCache().getLibGroupList(userInfo, userInstance);
            }
        } catch (Exception t) {
            log.error("Cannot load list of lib groups for user {} and exploratory {}", userInfo.getName(), exploratoryName, t);
            throw new DlabException("Cannot load list of libraries groups: " + t.getLocalizedMessage(), t);
        }
    }

    /**
     * Returns the list of installed libraries or libraries that were tried to be installed for exploratory with its's statuses.
     *
     * @param userInfo        user info.
     * @param exploratoryName of exploratory image.
     */
    @GET
    @Path("/lib_list")
    public List<LibInfoRecord> getLibList(@Auth UserInfo userInfo,
                                          @QueryParam("exploratory_name") @NotBlank String exploratoryName) {

        log.debug("Loading list of libraries for user {} and exploratory {}", userInfo.getName(), exploratoryName);
        try {
            return libraryService.getLibInfo(userInfo.getName(), exploratoryName);
        } catch (Exception t) {
            log.error("Cannot load list of libraries for user {} and exploratory {}", userInfo.getName(), exploratoryName, t);
            throw new DlabException("Cannot load list of libraries: " + t.getLocalizedMessage(), t);
        }
    }

    /**
     * Install the libraries to the exploratory environment.
     *
     * @param userInfo user info.
     * @param formDTO  description of libraries which will be installed to the exploratory environment.
     * @return Invocation response as JSON string.
     */
    @POST
    @Path("/lib_install")
    public Response libInstall(@Auth UserInfo userInfo, @Valid @NotNull LibInstallFormDTO formDTO) {
        log.debug("Installing libs to environment {} for user {}", formDTO, userInfo.getName());
        try {

            LibraryInstallDTO dto = libraryService.generateLibraryInstallDTO(userInfo, formDTO);
            String uuid;

            if (StringUtils.isEmpty(formDTO.getComputationalName())) {
                uuid = provisioningService.post(ExploratoryAPI.EXPLORATORY_LIB_INSTALL, userInfo.getAccessToken(),
                        libraryService.prepareExploratoryLibInstallation(userInfo.getName(), formDTO, dto), String.class);
            } else {
                uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_LIB_INSTALL, userInfo.getAccessToken(),
                        libraryService.prepareComputationalLibInstallation(userInfo.getName(), formDTO, dto), String.class);
            }

            RequestId.put(userInfo.getName(), uuid);
            return Response.ok(uuid).build();
        } catch (DlabException e) {
            log.error("Cannot install libs to exploratory environment {} for user {}: {}",
                    formDTO.getNotebookName(), userInfo.getName(), e.getLocalizedMessage(), e);
            throw new DlabException("Cannot install libraries: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the list of available libraries for exploratory basing on search conditions provided in @formDTO.
     *
     * @param userInfo user info.
     * @param formDTO  search condition for find libraries for the exploratory environment.
     */
    @POST
    @Path("search/lib_list")
    public Map<String, String> getLibList(@Auth UserInfo userInfo, @Valid @NotNull SearchLibsFormDTO formDTO) {
        log.trace("Loading list of libs for user {} with condition {}", userInfo.getName(), formDTO);
        try {

            UserInstanceDTO userInstance;

            if (StringUtils.isNotEmpty(formDTO.getComputationalName())) {

                userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(),
                        formDTO.getNotebookName(), formDTO.getComputationalName());

                userInstance.setResources(userInstance.getResources().stream()
                        .filter(e -> e.getComputationalName().equals(formDTO.getComputationalName()))
                        .collect(Collectors.toList()));

            } else {
                userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
            }

            return ExploratoryLibCache.getCache().getLibList(userInfo, userInstance, formDTO.getGroup(), formDTO.getStartWith());
        } catch (Exception t) {
            log.error("Cannot load list of libs for user {} with condition {}",
                    userInfo.getName(), formDTO, t);
            throw new DlabException("Cannot load list of libraries: " + t.getLocalizedMessage(), t);
        }
    }
}
