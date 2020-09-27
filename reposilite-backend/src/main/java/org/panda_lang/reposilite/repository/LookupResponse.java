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

package org.panda_lang.reposilite.repository;

import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.function.Option;

final class LookupResponse {

    private static final LookupResponse EMPTY_RESPONSE = new LookupResponse();

    private final Option<String> value;
    private final Option<FileDetailsDto> fileDetails;
    private final Option<String> contentType;
    private final boolean attachment;

    LookupResponse(String contentType, String value) {
        this.value = Option.of(value);
        this.fileDetails = Option.none();
        this.contentType = Option.of(contentType);
        this.attachment = false;
    }

    LookupResponse(FileDetailsDto fileDetails) {
        this.value = Option.none();
        this.fileDetails = Option.of(fileDetails);
        this.contentType = Option.of(fileDetails.getContentType());
        this.attachment = !FilesUtils.isReadable(fileDetails.getName());
    }

    private LookupResponse() {
        this.value = Option.none();
        this.fileDetails = Option.none();
        this.contentType = Option.none();
        this.attachment = false;
    }

    boolean isAttachment() {
        return attachment;
    }

    Option<String> getContentType() {
        return contentType;
    }

    Option<FileDetailsDto> getFileDetails() {
        return fileDetails;
    }

    Option<String> getValue() {
        return value;
    }

    public static LookupResponse empty() {
        return EMPTY_RESPONSE;
    }

}
