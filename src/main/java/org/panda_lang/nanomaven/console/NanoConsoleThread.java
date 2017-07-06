/*
 * Copyright (c) 2017 Dzikoysk
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

package org.panda_lang.nanomaven.console;

import java.util.Scanner;

public class NanoConsoleThread extends Thread {

    private final NanoConsole console;
    private boolean interrupted;

    public NanoConsoleThread(NanoConsole nanoConsole) {
        this.console = nanoConsole;
    }

    @Override
    public void run() {
        Scanner in = new Scanner(System.in);

        while (!interrupted) {
            String command = in.nextLine();

            try {
                console.execute(command);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void interrupt() {
        this.interrupted = true;
        super.interrupt();
    }

}
