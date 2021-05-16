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
package org.panda_lang.reposilite.maven.repository.api

import org.panda_lang.reposilite.utils.FilesUtils.isReadable

internal class LookupResponse(
    private val value: String? = null,
    private val fileDetails: FileDetailsResponse? = null,
    private val contentType: String? = null,
    private val isAttachment: Boolean = false
) {

    companion object {

        val EMPTY_RESPONSE = LookupResponse()

        fun of(fileDetails: FileDetailsResponse) =
            LookupResponse(
                fileDetails = fileDetails,
                contentType = fileDetails.contentType,
                isAttachment = !isReadable(fileDetails.name)
            )

        fun of(contentType: String, value: String) =
            LookupResponse(
                value = value,
                contentType = contentType,
            )

    }

}