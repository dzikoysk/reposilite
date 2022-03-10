import { Box, Flex, Heading, Link, Text } from "@chakra-ui/react"
import { MDXRemote } from "next-mdx-remote"
import { getGuideCategories, readGuideById } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'
import MDX from "../../components/MDX"

const GuideMenu = ({ categories }) => {
  return (
    <>
      {categories.map(category => (
        <Box key={category.name} paddingBottom={3}>
          <Heading as='h1' size='sm' paddingBottom={3}>
            {category.name}
          </Heading>
          {category.content.map(guide => (
            <Box key={guide.id} marginBottom='1' paddingLeft={4}>
              <Link whiteSpace={'nowrap'} wordBreak={'keep-all'} href={`/guide/${guide.id}`}>
                {guide.title}
              </Link>
            </Box>
          ))}
        </Box>
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

export default function Guide({ categories, selected }) {
  return (
    <Layout>
      <Box maxW={{ base: '95vw', md: 'container.md', xl: 'container.xl' }} mx='auto'>
        <Flex direction={{ base: 'column', md: 'row' }}>
          <Box paddingTop='12' mx='auto' paddingLeft='6' paddingRight={{ base: 6, md: 16 }}>
            <GuideMenu categories={categories} />
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
  const categories = await getGuideCategories()
  const selectedGuide = await readGuideById(id)

  return {
    props: {
      categories,
      selected: selectedGuide
    }
  }
}

export async function getStaticPaths() {
  const categories = await getGuideCategories()

  return {
    paths: categories
      .flatMap(category => category.content)
      .map(guide => ({
        params: {
          id: guide.id
        }
      })),
    fallback: false
  }
}