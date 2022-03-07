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
          <Text paddingBottom='1'>{guide.title}</Text>
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
      <MDXRemote components={MDX} {...selected.content} />
    </> 
  )
}

export default function Guide({ guides, selected }) {
  return (
    <Layout>
      <Container maxW={{ base: 'container.sm', lg: 'container.lg' }} marginInlineStart={{ base: '0', md: 'auto' }}>
        <Flex justifyContent="centered">
          <Box paddingTop='6' paddingRight='16'>
            <GuideMenu guides={guides} />
          </Box>
          <Box>
            <GuideView selected={selected} />
          </Box>
        </Flex>
      </Container>
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