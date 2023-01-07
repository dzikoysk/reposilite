const createSemverComparator = (reversed, filter, toSemver) => {
  // Semver sorting
  // ~ https://github.com/substack/semver-compare/issues/1#issuecomment-594765531
  const compare = (rawA, rawB) => {
    const a = rawA.split('-')
    const b = rawB.split('-')
    const pa = a[0].split('.')
    const pb = b[0].split('.')
    
    for (let idx = 0; idx < 3; idx++) {
        const na = Number(pa[idx])
        const nb = Number(pb[idx])
        if (na > nb) return 1
        if (nb > na) return -1
        if (!isNaN(na) && isNaN(nb)) return 1
        if (isNaN(na) && !isNaN(nb)) return -1
    }
    if (a[1] && b[1]) {
        return a[1] > b[1] ? 1 : (a[1] < b[1] ? -1 : 0)
    }
    return !a[1] && b[1] ? 1 : (a[1] && !b[1] ? -1 : 0)
  }

  return (a, b) => {
    if (!filter(a, b)) return 0
    const result = compare(toSemver(a), toSemver(b))
    return reversed ? -result : result
  }
}

export {
  createSemverComparator
}