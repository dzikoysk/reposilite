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

package org.panda_lang.reposilite.error;

import net.dzikoysk.dynamiclogger.Journalist;
import net.dzikoysk.dynamiclogger.Logger;
import org.jetbrains.annotations.Nullable;
import org.panda_lang.utilities.commons.ArrayUtils;
import org.panda_lang.utilities.commons.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FailureService implements Journalist {

    private final Journalist journalist;
    private final Set<String> exceptions = ConcurrentHashMap.newKeySet();

    public FailureService(Journalist journalist) {
        this.journalist = journalist;
    }

    public void throwException(String id, Throwable throwable) {
        getLogger().error(id);
        getLogger().exception(throwable);

        exceptions.add(String.join(System.lineSeparator(),
                "failure " + id,
                throwException(throwable)
        ).trim());
    }

    private String throwException(@Nullable Throwable throwable) {
        return throwable == null ? StringUtils.EMPTY : String.join(System.lineSeparator(),
                "  by " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage(),
                "  at " + ArrayUtils.get(throwable.getStackTrace(), 0).map(StackTraceElement::toString).orElseGet("<unknown stacktrace>"),
                throwException(throwable.getCause())
        );
    }

    public boolean hasFailures() {
        return !exceptions.isEmpty();
    }

    public Collection<? extends String> getFailures() {
        return exceptions;
    }

    @Override
    public Logger getLogger() {
        return journalist.getLogger();
    }

}
