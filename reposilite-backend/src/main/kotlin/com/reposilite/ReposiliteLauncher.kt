/*
 * Copyright (c) 2022 dzikoysk
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
package com.reposilite

import picocli.CommandLine

fun createWithParameters(vararg args: String): Reposilite? {
    val parameters = ReposiliteParameters()
    CommandLine(parameters).execute(*args)

    if (parameters.usageHelpRequested) {
        CommandLine.usage(parameters, System.out)
        return null
    }

    if (parameters.versionInfoRequested) {
        println("Reposilite $VERSION")
        return null
    }

    return ReposiliteFactory.createReposilite(parameters)
}

fun main(args: Array<String>) {
    createWithParameters(*args)?.launch()
}