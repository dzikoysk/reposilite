import { Box, Container, Flex, Heading, Link, Text } from "@chakra-ui/react"
import { MDXRemote } from "next-mdx-remote"
import { getAllGuides } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'
import MDX from "../../components/MDX"

const GuideMenu = ({ guides }) => {
  return (
    <>
      {guides.map(guide => (
        <Link key={guide.id} href={`/guide/${guide.id}`}>
          <Text marginBottom='1' whiteSpace={'nowrap'} wordBreak={'keep-all'}>{guide.title}</Text>
        </Link>
      ))}
    </>
  )
}

const GuideView = ({ selected }) => {
  return (
    <>
      <Link>
        <Heading as='h1' paddingY='4'>{selected.title}</Heading>
      </Link>
      <MDXRemote
        maxWidth={'10vw'}
        components={MDX}
        {...selected.content}
      />
    </> 
  )
}

export default function Guide({ guides, selected }) {
  return (
    <Layout>
      <Box maxW={{ base: '95vw', md: 'container.md', xl: 'container.xl' }} mx='auto'>
        <Flex direction={{ base: 'column', md: 'row' }}>
          <Box paddingTop='12' mx='auto' paddingLeft='6' paddingRight={{ base: 6, md: 16 }}>
            <GuideMenu guides={guides} />
          </Box>
          <Box maxW='70%' mx='auto' paddingTop='6' paddingRight={{ base: 0, md: 6 }} position='relative'>
            <GuideView selected={selected} />
          </Box>
        </Flex>
      </Box>
    </Layout>
  )
}

export async function getStaticProps({ params: { id } }) {
  const guides = await getAllGuides()
  const selectedGuide = guides.find(element => element.id === id)

  return {
    props: {
      guides: guides,
      selected: selectedGuide
    }
  }
}

export async function getStaticPaths() {
  const guides = await getAllGuides()

  return {
    paths: guides.map((guideEntry, index) => ({
      params: {
        id: guideEntry.id
      }
    })),
    fallback: false
  }
}