import { Box, Breadcrumb, BreadcrumbItem, BreadcrumbLink, Button, Container, Divider, Flex, Heading, Select, Text } from "@chakra-ui/react"
import Layout from "../../components/layout/Layout"
import { getPlugins, readPluginById } from "../../helpers/mdx"
import { Link } from '../../components/Link'
import MDX from "../../components/MDX"
import { MDXRemote } from "next-mdx-remote"
import { ChevronRightIcon } from "@chakra-ui/icons"
import Head from "next/head"
import { useEffect, useState } from "react"
import xml2js from 'xml2js'
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { chakraColor } from "../../helpers/chakra-theme"

const GitHubLink = ({ url, children }) => {
  return (
    <>
      <Link color={'purple.300'} href={`https://github.com/${url}`}>{children}</Link>
    </>
  )
}
export default function Plugin({ plugin }) {
  const [metadata, setMetadata] = useState({
    metadata: {
      artifactId: [
        ''
      ],
      versioning: [
        {
          versions: []
        }
      ]
    }
  })
  const [version, setVersion] = useState('')
  const gavPath = plugin.gav.replace(/\./g, '/')
  const groupUrl = `https://${plugin.url}/releases/${gavPath}/`

  useEffect(() => {
    fetch(`${groupUrl}/maven-metadata.xml`)
      .then(response => response.text())
      .then(data => xml2js.parseString(data, (err, result) => {
        if (err) console.log(err)
        else console.log(result)
        setMetadata(result)
        setVersion(result?.metadata?.versioning[0]?.versions?.reverse()[0]?.version || '')
      }))
      .catch(err => console.log(err))
  }, [])

  const [ bg, bgCss ] = useColorModeValue('download-bg', chakraColor('gray.50'), chakraColor('gray.700'))
  const [ buttonBg, buttonBgCss ] = useColorModeValue('download-button-bg', chakraColor('purple.100'), chakraColor('purple.500'))

  return (
    <Layout>
      <Head>
        <title>{plugin.title} plugin · Plugins · Reposilite</title>  
      </Head>
      <ColorModeStyles styles={[bgCss, buttonBgCss]} />
      <Container maxW={'container.md'} marginTop={8}>
        <Flex flexDirection={{ base: 'column', md: 'row' }}>
          <Box minWidth={'60%'}>
            <Breadcrumb spacing='8px' separator={<ChevronRightIcon color='gray.500' />}>
              <BreadcrumbItem>
                <BreadcrumbLink href='/plugin'>Plugins</BreadcrumbLink>
              </BreadcrumbItem>
              <BreadcrumbItem isCurrentPage>
                  <BreadcrumbLink href={`/plugin/${plugin.id}`}>{plugin.title}</BreadcrumbLink>
              </BreadcrumbItem>
            </Breadcrumb>
            <Heading marginY={2}>{plugin.title}</Heading>
            <Text>
              Repository: <GitHubLink url={plugin.repository}>{plugin.repository}</GitHubLink>
            </Text>
            <Text>
              Authors: {plugin.authors.map(author => (
                <Text key={author} as='abbr'>
                  <GitHubLink url={author}>{author}</GitHubLink>
                  {[...plugin.authors].pop() !== author ? <>,&nbsp;</> : <></>}
                </Text>
              ))}
            </Text>
          </Box>
          <Box width='full'>
            <Box
              width={'full'}
              marginTop={{ base: '6', md: '4' }}
              marginBottom={{ base: '4', md: '0' }}
              backgroundColor={bg}
              paddingY={4}
              paddingX={4}
              borderRadius={'xl'}
            >
              <Select
                value={version}
                onChange={(version) => { console.log(version); setVersion(version); }}
                height={'8'}
                width={'full'}
              >
                {metadata.metadata.versioning[0].versions.reverse().map(versionEntry => (
                  <option
                    key={versionEntry.version}
                    value={versionEntry.version}
                    width={'full'}
                  >
                    {versionEntry.version}
                  </option>
                ))}
              </Select>
              <Link href={`${groupUrl}/${version}/${metadata.metadata.artifactId[0]}-${version}-all.jar`}>
                <Button
                  width={'full'}
                  marginTop={'2'}
                  height={'8'}
                  backgroundColor={buttonBg}
                >
                  Download
                </Button>
              </Link>
            </Box>
          </Box>
        </Flex>
        <Divider paddingY={2} marginBottom={1} />
        <MDXRemote components={MDX} {...plugin.content} />
      </Container>
    </Layout>
  )
}

export async function getStaticProps({ params: { id } }) {
  const plugin = await readPluginById(id)

  return {
    props: {
      plugin
    }
  }
}

export async function getStaticPaths() {
  const plugins = await getPlugins()

  return ({
    paths: plugins.map(plugin => ({
      params: {
        id: plugin.id
      }
    })),
    fallback: false
  })
}
