import { Box, Flex, Heading, Link, Text } from "@chakra-ui/react";

const feature = (title, description) =>
  ({ title, description })

const features = [
  feature('Authorization', 'Personal access-token based authorization with configurable routes and permissions'),
  feature('Docker 🐋', <Text>Available docker images <Link>docker.com/reposilite</Link></Text>),
  feature('Dashboard', 'Simple dashboard with repository browser and management panel'),
  feature('API Endpoints', 'Exposes dedicated REST API to communicate with repository using external tools'),
  feature('Plugin system', 'Extend your instance with personalized extensions in Java, Kotlin or Groovy'),
  feature('Storage providers', 'Store artifacts locally or integrate your instance with cloud object storage like AWS S3'),
  feature('Proxy', 'Link other Maven repositories and redirect all traffic through your instance'),
  feature('And...', 'Much more, take a look at Guide section to learn more!')
]

export default function Features() {
  return (
    <Flex justifyContent={'space-between'} flexWrap='wrap' backgroundColor={'gray.50'} borderRadius='2xl' padding={'7'}>
      {features.map(({ title, description }) => (
        <Box width={'33%'} textAlign='center' paddingY='2'>
          <Heading as='h4' size='sm'>{title}</Heading>
          <Box paddingY='5' paddingX='6' fontSize={'sm'}>
            {description}
          </Box>
        </Box>
      ))}
    </Flex>
  )
}