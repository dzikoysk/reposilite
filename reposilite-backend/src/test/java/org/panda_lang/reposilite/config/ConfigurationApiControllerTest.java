/*
 * Copyright (c) 2020 Dzikoysk
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

package org.panda_lang.reposilite.config;

import com.google.api.client.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.hjson.JsonObject;
import org.junit.jupiter.api.Test;
import org.panda_lang.reposilite.ReposiliteIntegrationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigurationApiControllerTest extends ReposiliteIntegrationTest {

    @Test
    void shouldReturn200AndConfigurationDto() throws IOException {
        HttpResponse response = super.get("/api/configuration");
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        JsonObject configurationDto = (JsonObject) JsonObject.readJSON(response.parseAsString());
        assertNotNull(configurationDto.getString("title", null));
        assertNotNull(configurationDto.getString("description", null));
        assertNotNull(configurationDto.getString("accentColor", null));
    }

}