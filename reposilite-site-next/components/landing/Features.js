import { Box, Flex, Heading, Link, Text } from "@chakra-ui/react";
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode";
import { chakra } from "../../helpers/chakra-theme";

const feature = (title, description) =>
  ({ title, description })

const features = [
  feature('Open Source', 'Personal access-token based authorization with configurable routes and permissions'),
  feature('Docker 🐋', <Text>Available docker images <Link>docker.com/reposilite</Link></Text>),
  feature('Authorization', 'Personal access-token based authorization with configurable routes and permissions'),
  feature('Dashboard', 'Simple dashboard with repository browser and management panel'),
  feature('API Endpoints', 'Exposes dedicated REST API to communicate with repository using external tools'),
  feature('Plugin system', 'Extend your instance with personalized extensions in Java, Kotlin or Groovy'),
  feature('Storage providers', 'Store artifacts locally or integrate your instance with cloud object storage like AWS S3'),
  feature('Proxy', 'Link other Maven repositories and redirect all traffic through your instance'),
  feature('And...', 'Much more, take a look at Guide section to learn more!')
]

// TODO - Better layout or sth idk
export default function Features() {
  const [featuresBg, featuresBgCss] = useColorModeValue('features-bg', chakra('colors-gray-50'), chakra('colors-gray-900'))
  
  return (
    <>
      <ColorModeStyles styles={[featuresBgCss]} />
      <Flex
        justifyContent={'space-between'}
        flexWrap='wrap'
        borderRadius='2xl'
        padding={'7'}
        style={{ backgroundColor: featuresBg }}
      >
        {features.map(({ title, description }) => (
          <Box key={title} width={'33%'} textAlign='center'>
            <Heading as='h4' size='sm' paddingTop='5'>{title}</Heading>
            <Box paddingY='3' paddingX='6' fontSize={'sm'}>
              {description}
            </Box>
          </Box>
        ))}
      </Flex>
    </>
  )
}