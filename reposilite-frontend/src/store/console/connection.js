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

import { ref } from "vue"
import { createURL } from '../client'
import { EventSource as Eventsource } from 'extended-eventsource';
import { useSession } from "../session.js";

const { client } = useSession()

const connection = ref()
const command = ref("")

export default function useConsole() {
  // TODO: does this need better endpoint name?
  const consoleAddress = createURL("/api/console/log");

  const isConnected = () => {
    // TODO: stop using built-in EventSource for readystate constants
    //  (the ones from extended-eventsource return undefined for some reason)
    return connection.value?.readyState === EventSource.OPEN
  }

  const close = () => {
    if (isConnected())
      connection.value.close()
  }

  const history = ref([''])
  const historyIdx = ref(0)

  const addCommandToHistory = (command) => {
    if (history.value[history.value.length - 1] == '')
      history.value.pop()
    history.value.push(command)
    historyIdx.value = history.value.length - 1
  }

  const execute = () => {
    addCommandToHistory(command.value)
    client.value.console.execute(command.value)
    command.value = ''
  }

  const previousCommand = () =>
    traverseHistory(-1)
  
  const nextCommand = () =>
    traverseHistory(1)

  const traverseHistory = (direction) => {
    const currentCommand = command.value
    const commands = history.value
    const lastCommandIdx = commands.length - 1

    if (lastCommandIdx === historyIdx.value && commands[lastCommandIdx] !== currentCommand)
      addCommandToHistory(currentCommand)
    
    historyIdx.value = Math.max(0, Math.min(commands.length - 1, historyIdx.value + direction))
    command.value = history.value[historyIdx.value]
  }

  // this is needed to stop an error from appearing in console when
  // switching/refreshing the page without closing the connection
  // TODO: move somewhere else?
  window.onbeforeunload = function () {
    close();
  };

  const onOpen = ref()
  const onMessage = ref()
  const onError = ref()
  const onClose = ref()

  const connect = (token) => {
    try {
      connection.value = new Eventsource(consoleAddress, {
        headers: {
          Authorization: `xBasic ${btoa(`${token.name}:${token.secret}`)}`
        },
        // TODO: should we try to reconnect? how many attempts should be made?
        disableRetry: true
      })

      connection.value.onopen = () => {
        onOpen?.value()
      }

      connection.value.addEventListener("log", (event) => {
        if (!event.data.toString().includes("GET /api/status/instance from"))
          onMessage?.value(event.data)
      })

      connection.value.onerror = (error) => {
        onError?.value(error)
      }

      connection.value.onclose = () =>
        onClose?.value()

    } catch (error) {
      onError?.value(error)
    }
  }

  return {
    connection,
    connect,
    close,
    onOpen,
    onMessage,
    onError,
    onClose,
    command,
    execute,
    previousCommand,
    nextCommand,
    isConnected
  }
}
