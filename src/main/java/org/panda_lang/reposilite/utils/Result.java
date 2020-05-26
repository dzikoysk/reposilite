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

package org.panda_lang.reposilite.utils;

import io.vavr.control.Option;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Result<V, E> {

    private final @Nullable V value;
    private final @Nullable E error;

    Result(@Nullable V value, @Nullable E error) {
        this.value = value;
        this.error = error;
    }

    public Result<V, E> orElse(Function<E, Result<V, E>> orElse) {
        return isDefined() ? this : orElse.apply(error);
    }

    public boolean isDefined() {
        return getValue().isDefined();
    }

    public Option<V> getValue() {
        return Option.of(value);
    }

    public Option<E> getError() {
        return Option.of(error);
    }

    public static <V, E> Result<V, E> ok(V value) {
        return new Result<>(value, null);
    }

    public static <V, E> Result<V, E> error(E err) {
        return new Result<>(null, err);
    }

}
