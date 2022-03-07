export function chakra(property) {
  return `var(--chakra-${property})`
}

export function chakraColor(color) {
  return chakra(`colors-${color.replace('.', '-')}`)
}