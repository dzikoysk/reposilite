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
import com.reposilite.storage.api.UnsupportedLocationException
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LocationTest {

    @Test
    fun `should properly append paths`() {
        assertThat(Location.of("group").resolve("artifact").toString()).isEqualTo("group/artifact")
        assertThat(Location.of("/group").resolve("/artifact").toString()).isEqualTo("group/artifact")
        assertThat(Location.of("/////group/////").resolve("/////artifact/////").toString()).isEqualTo("group/artifact")
        assertThat(Location.of(".abc").resolve("_cdf.efg").toString()).isEqualTo(".abc/_cdf.efg")
        assertThat(Location.of("시험").resolve("기준").toString()).isEqualTo("시험/기준")
    }

    @Test
    fun `should normalize benign surrounding and redundant slashes`() {
        assertThat(Location.of("/artifact").toPath().toString()).isEqualTo("artifact")
        assertThat(Location.of("group//artifact").toString()).isEqualTo("group/artifact")
        assertThat(Location.of(".").toPath().toString()).isEqualTo("")
        assertThat(Location.of("/").toPath().toString()).isEqualTo("")
        assertThat(Location.of("").toPath().toString()).isEqualTo("")
    }

    @Test
    fun `should canonicalize trusted file-system paths`() {
        assertThat(Location.of(Path.of("group/artifact")).toString()).isEqualTo("group/artifact")
        assertThat(Location.of(Path.of("group"), Path.of("group/artifact/file.jar")).toString()).isEqualTo("artifact/file.jar")
    }

    @Test
    fun `should reject paths with illegal characters or path operators`() {
        assertThatThrownBy { Location.of("group/../artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("..") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("....//....//artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/c:/artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("C:/artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group\\artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("..\\..\\artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/<artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/x>/artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/\"x/artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/'x/artifact") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/a?b") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/a*b") }.isInstanceOf(UnsupportedLocationException::class.java)
        assertThatThrownBy { Location.of("group/a|b") }.isInstanceOf(UnsupportedLocationException::class.java)
    }

    @Test
    fun `should reject untrusted requests with illegal characters or operators`() {
        assertThat(Location.ofRequest("group/c:/artifact").isErr).isTrue()
        assertThat(Location.ofRequest("group/../../artifact").isErr).isTrue()
        assertThat(Location.ofRequest("group/<artifact").isErr).isTrue()
        assertThat(Location.ofRequest("group\\artifact").isErr).isTrue()
        assertThat(Location.ofRequest("group/artifact\r\nDEPLOY").isErr).isTrue()
        assertThat(Location.ofRequest("group/artifact").get().toString()).isEqualTo("group/artifact")
        assertThat(Location.ofRequest("/group//artifact/").get().toString()).isEqualTo("group/artifact")
    }

}
