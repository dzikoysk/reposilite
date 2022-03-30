import { Box, Button, Divider, Flex, Heading, Text } from "@chakra-ui/react"
import Head from "next/head"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import Layout from "../components/layout/Layout"
import { Link } from "../components/Link"
import { chakraColor } from "../helpers/chakra-theme"

const individualCards = [
  { 
    title: 'Star project',
    description: 'Star project on GitHub to help us reach a wider audience',
    buttons: [
      {
        title: 'Star Reposilite',
        link: 'https://github.com/dzikoysk/reposilite/stargazers'
      },
      {
        title: 'Follow @dzikoysk',
        link: 'https://twitter.com/dzikoysk'
      }
    ]
  },
  {
    title: 'Donation',
    description: 'Consider a donation to financially support my work and ecosystem around',
    buttons: [
      {
        title: 'GitHub Sponsors',
        link: 'https://github.com/sponsors/dzikoysk'
      },
      {
        title: 'Panda organization',
        link: 'https://panda-lang.org/support'
      }
    ]
  },
  {
    title: 'Contribute',
    description: 'Join developers team and develop projects associated with Reposilite project',
    buttons: [
      {
        title: 'Visit issues',
        link: 'https://github.com/dzikoysk/reposilite/issues'
      },
      {
        title: 'Visit PRs',
        link: 'https://github.com/dzikoysk/reposilite/pulls'
      }
    ]
  }
]

const IndividualCard = ({ title, description, buttons }) => {
  const [cardBg, cardBgCss] = useColorModeValue('individual-card-bg', chakraColor('gray.100'), chakraColor('gray.900'))
  const [cardButtonBg, cardButtonBgCss] = useColorModeValue('individual-card-button-bg', chakraColor('gray.200'), chakraColor('gray.700'))

  return (
    <>
      <ColorModeStyles styles={[cardBgCss, cardButtonBgCss]} />
      <Flex
        flexDirection={'column'}
        justifyContent={'space-between'}
        backgroundColor={cardBg}
        paddingX={8}
        paddingY={6}
        marginX={4}
        borderRadius={'lg'}
        w={'30%'}
      >
        <Heading as='h2' size={'sm'} textAlign={'center'}>{title}</Heading>
        <Text marginY={4} textAlign={'center'}>{description}</Text>
        {buttons.map(button => (
          <Button
            key={button.title}
            marginTop={2}
            backgroundColor={cardButtonBg}
            _hover={{ backgroundColor: cardButtonBg }}
          >
            <Link href={button.link}>{button.title}</Link>
          </Button>
        ))}
      </Flex>
    </>
  )
}

const organizationCards = [
  {
    title: 'Sponsor',
    description: (
      <Text>
        If you'd like to invest into open source sector, feel free to contact me using one of conversation channels listed on&nbsp;
        <Link href={'https://dzikoysk.net/#contact'} color={'purple.400'}>dzikoysk.net</Link>.
        I'm open to discuss various possibilities and scenarios individually, so we can find out the best solution for both sides! :)
      </Text>
    )
  }
]

const OrganizationCard = ({ title, description }) => {
  const [cardBg, cardBgCss] = useColorModeValue('org-card-bg', chakraColor('gray.100'), chakraColor('gray.900'))

  return (
    <>
      <ColorModeStyles styles={[cardBgCss]} />
      <Flex backgroundColor={cardBg} padding={6} marginBottom={8} w='full' flexDirection={'column'}>
        <Heading as='h2' size={'sm'}>{title}</Heading>
        <Text paddingTop={3}>{description}</Text>
      </Flex>
    </>
  )
}

export default function Home() {
  return (
    <Layout>
      <Head>
        <title>Support · Reposilite</title>  
      </Head>
      <Flex flexDirection={'column'} maxW={'container.lg'} px={'10'} mx={'auto'}>
        <Flex flexDirection={'column'} textAlign={'center'} justifyContent={'center'} paddingTop={14} paddingBottom={8}>
          <Heading  as={'h1'} size={'lg'}>How to help? 💕</Heading>
          <Box paddingTop={3}>
            Reposilite project and associated components are fully open source initiative.
            <br />
            It's currently mainly maintained by&nbsp;
            <Link color={'purple.400'} href={'https://twitter.com/dzikoysk'}>@dzikoysk</Link>
          </Box>
        </Flex>
        <Heading textAlign={'center'} size={'md'} paddingBottom={10}>For individuals</Heading>
        <Flex justifyContent={'space-between'}>
          {individualCards.map(card => (
            <IndividualCard key={card.title} {...card} />
          ))}
        </Flex>
        <Heading textAlign={'center'} size={'md'} paddingTop={10} paddingBottom={12}>For organizations</Heading>
        <Flex flexDirection={'column'}>
          {organizationCards.map(card => (
            <OrganizationCard key={card.title} {...card} />
          ))}
        </Flex>
      </Flex>
    </Layout>
  )
}