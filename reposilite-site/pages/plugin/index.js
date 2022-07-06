import { Badge, Box, Flex, Heading, Text } from "@chakra-ui/react"
import { getPlugins } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'
import { Link } from '../../components/Link'
import { useEffect, useState } from "react"
import { StarIcon } from "@chakra-ui/icons"
import { chakraColor } from "../../helpers/chakra-theme"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import Head from "next/head"

export default function Guide({ plugins }) {
  const [loadedPlugins, setLoadedPlugins] = useState([])

  useEffect(() => {
    plugins.forEach(plugin => {
      setLoadedPlugins([]) // required for dev env reloads

      fetch(`https://api.github.com/repos/${plugin.repository}`)  
        .then(response => response.json())
        .then(data => plugin.stars = data.stargazers_count)
        .then(() => setLoadedPlugins(loadedPlugins => [...loadedPlugins, plugin]))
        .catch(err => console.log(err))
    })
  }, [])

  useEffect(() => {
    loadedPlugins.sort((a, b) => {
      let result = a.stars - b.stars
      if (result == 0) result = (a.id < b.id) ? -1 : 1
      return result
    })
  }, [loadedPlugins])

  const [cardBg, cardBgCss] = useColorModeValue('plugin-card-bg', chakraColor('gray.50'), chakraColor('gray.900'))
  const [cardBorder, cardBorderCss] = useColorModeValue('plugin-card-border', chakraColor('gray.200'), 'black')

  return (
    <Layout>
      <Head>
        <title>Plugins · Reposilite</title>  
      </Head>
      <ColorModeStyles styles={[cardBgCss, cardBorderCss]} />
      <Box maxW={{ base: '95vw', md: 'container.md' }} mx='auto'>
        <Flex direction={{ base: 'column' }} justifyContent={'space-around'}>
          {loadedPlugins.map(plugin => (
            <Box
              key={plugin.id}
              marginTop={6} 
              w='full'
              border='1px'
              borderColor={cardBorder}
              backgroundColor={cardBg}
              borderRadius='lg'
              padding={4}
              display='flex'
              direction='row'
            >
              <Box textAlign={'center'}>
                <Text>
                  {plugin.stars}
                  <StarIcon marginLeft={1} marginBottom={1} color={'purple.300'} />
                </Text>
                <Text>
                  <Link href={`https://github.com/${plugin.repository}`}>
                    {plugin.repository}
                  </Link>
                </Text>
              </Box>
              <Box paddingLeft={7}>
                <Flex>
                  <Heading size={'md'}>
                    <Link href={`/plugin/${plugin.id}`}>{plugin.title}</Link>
                  </Heading>
                  <Text paddingX={4} paddingBottom={0.5}>
                    {plugin.official
                      ? <Badge colorScheme={'purple'} variant={'outline'}>Official</Badge>
                      : <Badge>3rd party</Badge>}
                  </Text>
                </Flex>
                <Text>{plugin.description}</Text>
              </Box>
            </Box>
          ))}
        </Flex>
      </Box>
    </Layout>
  )
}

export async function getStaticProps() {
  const plugins = await getPlugins()

  return {
    props: {
      plugins
    }
  }
}
