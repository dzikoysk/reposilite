import { Badge, Box, Flex, Heading, Link, Text } from "@chakra-ui/react"
import { getPlugins } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'
import { useEffect, useState } from "react"
import { StarIcon } from "@chakra-ui/icons"
import { chakraColor } from "../../helpers/chakra-theme"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"

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
    loadedPlugins.sort((a, b) => a.stars - b.stars)
  }, [loadedPlugins])

  const [cardBg, cardBgCss] = useColorModeValue('plugin-card-bg', chakraColor('gray.50'), chakraColor('gray.900'))
  const [cardBorder, cardBorderCss] = useColorModeValue('plugin-card-border', chakraColor('gray.200'), 'black')

  return (
    <Layout>
      <ColorModeStyles styles={[cardBgCss, cardBorderCss]} />
      <Box maxW={{ base: '95vw', md: 'container.md', xl: 'container.xl' }} mx='auto'>
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
                <Link href={`/plugin/${plugin.id}`}>
                  <Flex>
                    <Heading size={'md'}>{plugin.title}</Heading>
                    <Text paddingX={4} paddingBottom={0.5}>
                      {plugin.official
                        ? <Badge colorScheme={'purple'} variant={'outline'}>Official</Badge>
                        : <Badge>3rd party</Badge>}
                    </Text>
                  </Flex>
                  <Text>{plugin.description}</Text>
                </Link>
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