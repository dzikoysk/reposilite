import { Box, Breadcrumb, BreadcrumbItem, BreadcrumbLink, Flex, Heading, Link, Text } from "@chakra-ui/react"
import { MDXRemote } from "next-mdx-remote"
import { getGuideCategories, readGuideById } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'
import MDX from "../../components/MDX"
import { ChevronRightIcon } from "@chakra-ui/icons"
import Head from "next/head"

const GuideMenu = ({ categories }) => {
  return (
    <>
      {categories.map(category => (
        <Box key={category.name} paddingBottom={2} paddingTop={1}>
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
  const { id, title, content } = selected
  const guideUrl = `/guide/${id}`

  return (
    <>
      <Breadcrumb spacing='8px' separator={<ChevronRightIcon color='gray.500' />}>
        <BreadcrumbItem>
          <BreadcrumbLink href='/guide/about'>Guide</BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbItem isCurrentPage>
            <BreadcrumbLink href={guideUrl}>{title}</BreadcrumbLink>
        </BreadcrumbItem>
      </Breadcrumb>
      <Box paddingY='4' paddingTop={6}>
        <Heading as='h1'>
          <Link href={guideUrl}>
            {title}
          </Link>
        </Heading>
      </Box>
      <MDXRemote
        maxWidth={'10vw'}
        components={MDX}
        {...content}
      />
    </> 
  )
}

export default function Guide({ categories, selected }) {
  return (
    <Layout>
      <Head>
        <title>{selected.title} · Guide · Reposilite</title>  
      </Head>
      <Box maxW={{ base: '95vw', md: 'container.md', xl: 'container.xl' }} mx='auto'>
        <Flex direction={{ base: 'column', md: 'row' }}>
          <Box paddingTop='24' mx='auto' paddingLeft='6' paddingRight={{ base: 6, md: 16 }}>
            <GuideMenu categories={categories} />
          </Box>
          <Box maxW='70%' mx='auto' paddingTop='10' paddingRight={{ base: 0, md: 6 }} position='relative'>
            <GuideView selected={selected} />
          </Box>
        </Flex>
      </Box>
    </Layout>
  )
}

export async function getStaticProps({ params: { id } }) {
  const categories = await getGuideCategories()
  const selectedCategory = categories.find(category => category.content.find(guide => guide.id == id))
  const selectedGuide = await readGuideById(selectedCategory.directory, id)

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