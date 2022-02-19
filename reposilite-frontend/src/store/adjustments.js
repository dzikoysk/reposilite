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