/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class ExploratoryLibListDTO extends StatusBaseDTO<ExploratoryLibListDTO> {

    @JsonProperty
    private String libs;

    @JsonProperty
    private String imageName;

    public String getLibs() {
        return libs;
    }

    public void setLibs(String libs) {
        this.libs = libs;
    }

    public ExploratoryLibListDTO withLibs(String libs) {
        setLibs(libs);
        return this;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public ExploratoryLibListDTO withImageName(String imageName) {
        setImageName(imageName);
        return this;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    			.add("imageName", imageName)
    			.add("libs", (libs == null ? "null" : "..."));
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
