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

import { ref, watchEffect } from 'vue'
import semver from 'semver'
import semverRegex from 'semver-regex'

const reversedFileOrder = ref(localStorage.getItem('reversedFileOrder') === 'true')
watchEffect(() => localStorage.setItem('reversedFileOrder', reversedFileOrder.value))

const displayHashFiles = ref(localStorage.getItem('displayHashFiles') === 'true')
watchEffect(() => localStorage.setItem('displayHashFiles', displayHashFiles.value))

export function useAdjustments() {
  const applyAdjustments = (files) => {
    if (!displayHashFiles.value) {
      files = files.filter(file => 
        !['.md5', '.sha1', '.sha256', '.sha512'].some(ext => file.name.endsWith(ext))
      )
    }
  
    if (reversedFileOrder.value) {
      try {
        files = files.sort((a, b) => {
          const aIsDirectory = a.type === 'DIRECTORY'
          const bIsDirectory = b.type === 'DIRECTORY'
  
          if (aIsDirectory && bIsDirectory) {
            const sv1 = semverRegex().exec(a.name)[0] || a.name
            const sv2 = semverRegex().exec(b.name)[0] || b.name
            return semver.rcompare(sv1, sv2)
          }
  
          return 0 // preserve default order of unknown entires
        })
      } catch (error) { 
        // just don't sort unknown entries
      }
    }
    
    return files
  }

  return {
    reversedFileOrder,
    displayHashFiles,
    applyAdjustments
  }
}