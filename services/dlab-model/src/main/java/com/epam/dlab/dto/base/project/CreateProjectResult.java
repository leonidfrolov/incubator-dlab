package com.epam.dlab.dto.base.project;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateProjectResult extends StatusBaseDTO<CreateProjectResult> {
	private ProjectEdgeInfo edgeInfo;
	@JsonProperty("project_name")
	private String projectName;

}