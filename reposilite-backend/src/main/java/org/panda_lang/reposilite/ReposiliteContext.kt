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
package org.panda_lang.reposilite

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.auth.Session
import org.panda_lang.utilities.commons.function.Option
import org.panda_lang.utilities.commons.function.ThrowingConsumer
import org.panda_lang.utilities.commons.function.ThrowingSupplier
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ReposiliteContext(
    private val journalist: Journalist,
    val uri: String,
    val method: String,
    val address: String,
    val header: Map<String, String>,
    val session: Session?,
    private val input: ThrowingSupplier<InputStream, IOException>
) : Journalist {

    private var result: ThrowingConsumer<OutputStream, IOException>? = null

    fun result(result: ThrowingConsumer<OutputStream, IOException>?) {
        this.result = result
    }

    fun result(): Option<ThrowingConsumer<OutputStream, IOException>?> =
        Option.of(result)

    fun input(): InputStream =
        input.get()

    override fun getLogger(): Logger =
        journalist.logger

}