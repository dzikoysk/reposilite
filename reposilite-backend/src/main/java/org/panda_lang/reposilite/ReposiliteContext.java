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

package org.panda_lang.reposilite;

import net.dzikoysk.dynamiclogger.Journalist;
import net.dzikoysk.dynamiclogger.Logger;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.ThrowingConsumer;
import org.panda_lang.utilities.commons.function.ThrowingSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public final class ReposiliteContext implements Journalist {

    private final Journalist journalist;
    private final String uri;
    private final String method;
    private final String address;
    private final Map<String, String> header;
    private final ThrowingSupplier<InputStream, IOException> input;
    private ThrowingConsumer<OutputStream, IOException> result;

    public ReposiliteContext(
            Journalist journalist,
            String uri,
            String method,
            String address,
            Map<String, String> header,
            ThrowingSupplier<InputStream, IOException> input) {

        this.journalist = journalist;
        this.uri = uri;
        this.method = method;
        this.address = address;
        this.header = header;
        this.input = input;
    }

    public void result(ThrowingConsumer<OutputStream, IOException> result) {
        this.result = result;
    }

    public Option<ThrowingConsumer<OutputStream, IOException>> result() {
        return Option.of(result);
    }

    public InputStream input() throws IOException {
        return input.get();
    }

    public Map<String, String> headers() {
        return header;
    }

    public String address() {
        return address;
    }

    public String method() {
        return method;
    }

    public String uri() {
        return uri;
    }

    @Override
    public Logger getLogger() {
        return journalist.getLogger();
    }

}
