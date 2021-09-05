const parser = new DOMParser()

export default function useMetadata() {
  const parseMetadata = (source) =>
    parser.parseFromString(source, 'text/xml')
  
  const groupId = (metadata) =>
    metadata
      ?.getElementsByTagName('groupId')[0]
      ?.firstChild
      ?.nodeValue
  
  const artifactId = (metadata) =>
    metadata
      ?.getElementsByTagName('artifactId')[0]
      ?.firstChild
      ?.nodeValue
  
  const versions = (metadata) =>
    Array.from(
      metadata
        ?.getElementsByTagName('versioning')[0]
        ?.children)
      ?.map(node => node.firstChild.nodeValue)
      ?? ['{unknown}']

  return {
    parseMetadata,
    groupId,
    artifactId,
    versions
  }
}