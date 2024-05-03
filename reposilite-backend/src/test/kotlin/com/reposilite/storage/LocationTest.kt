/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.storage

import com.reposilite.storage.api.Location
import java.io.File
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocationTest {

    @Test
    fun `should properly append paths`() {
        assertThat(Location.of("group").resolve("artifact").toString()).isEqualTo("group/artifact")
        assertThat(Location.of("/group").resolve("/artifact").toString()).isEqualTo("group/artifact")
        assertThat(Location.of("/////group/////").resolve("/////artifact/////").toString()).isEqualTo("group/artifact")
        assertThat(Location.of("\\\\\\group\\\\\\").resolve("\\\\\\artifact\\\\\\").toString()).isEqualTo("group/artifact")
        assertThat(Location.of("시험").resolve("기준").toString()).isEqualTo("시험/기준")
    }

    @Test
    fun `should normalize corrupted paths`() {
        assertThat(Location.of(Path.of("../../artifact")).toPath().get().toString()).isEqualTo("artifact")
        assertThat(Location.of(Path.of("C:/artifact")).toPath().get().toString()).isEqualTo("artifact")
        assertThat(Location.of("artifact").resolve("../../root").toPath().get().toString()).isEqualTo("artifact" + File.separator + "root")
    }

}
