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
import { fetchEventSource } from "@microsoft/fetch-event-source"
import {useSession} from "../session.js";

const { client } = useSession()

const connected = ref()
const command = ref("")

export default function useConsole() {
  // TODO: does this need better endpoint name?
  const consoleAddress = createURL("/api/console/log");

  let abortController;

  const isConnected = () => connected.value

  const close = () => {
    abortController?.abort("Closing connection")
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
        .catch(error => console.log(error))
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

  const onOpen = ref()
  const onMessage = ref()
  const onError = ref()
  const onClose = ref()

  const connect = (token) => {
    const abortController = new AbortController()

    try {
      fetchEventSource(consoleAddress, {
        signal: abortController.signal,
        openWhenHidden: true,
        headers: {
          Authorization: `xBasic ${btoa(`${token.name}:${token.secret}`)}`
        },
        onopen: async (response) => {
          //connection.value.send(`Authorization:${token.name}:${token.secret}`)
          connected.value = true
          onOpen?.value()
        },
        onmessage: (message) => {
          //if (message.data != "keep-alive" && !message.data.toString().includes("GET /api/status/instance from"))
          if (message.event === "log")
            onMessage?.value(message.data)
        },
        onerror: (error) => {
          onError?.value(error)
        },
        onclose: () => {
          onClose?.value()
          connected.value = false
        }
      })
    } catch (error) {
      onError?.value(error)
    }
  }

  return {
    //connection,
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
