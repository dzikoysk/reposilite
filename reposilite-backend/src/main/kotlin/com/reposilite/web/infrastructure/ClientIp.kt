/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.web.infrastructure

import io.javalin.http.Header.X_FORWARDED_FOR

internal fun resolveClientIp(forwardedIp: String, peerIp: String, header: (String) -> String?): String {
    val forwarded = forwardedIp.takeIf { it.isNotBlank() }?.let(header) ?: return peerIp
    val clientIp = when {
        forwardedIp.equals(X_FORWARDED_FOR, ignoreCase = true) -> forwarded.substringAfterLast(',')
        ',' in forwarded -> return peerIp
        else -> forwarded
    }

    return clientIp.trim().ifEmpty { peerIp }
}
