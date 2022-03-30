import {
  Box,
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  Button,
  Divider,
  Drawer,
  DrawerBody,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  Heading,
  LinkBox,
  Text,
  useBreakpointValue,
  useDisclosure,
} from "@chakra-ui/react"
import { MDXRemote } from "next-mdx-remote"
import { getGuideCategories, readGuideById } from "../../helpers/mdx"
import Layout from "../../components/layout/Layout"
import MDX from "../../components/MDX"
import { Link, LinkOverlay } from "../../components/Link"
import { ChevronRightIcon, EditIcon, HamburgerIcon } from "@chakra-ui/icons"
import Head from "next/head"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { chakraColor } from "../../helpers/chakra-theme"

const TableOfContents = ({ categories, onClick }) => {
  return (
    <Flex flexDirection={'column'}>
      {categories.map((category) => (
        <Box key={category.name} paddingBottom={2} paddingTop={1}>
          <Heading as="h1" size="sm" paddingBottom={3}>
            {category.name}
          </Heading>
          {category.content.map((guide) => (
            <Box key={guide.id} marginBottom="1" paddingLeft={4}>
              <Link
                whiteSpace={"nowrap"}
                wordBreak={"keep-all"}
                href={`/guide/${guide.id}`}
                onClick={onClick}
              >
                {guide.title}
              </Link>
            </Box>
          ))}
        </Box>
      ))}
    </Flex>
  )
}

const GuideMenu = ({ categories }) => {
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [ menuBg, menuBgCss ] = useColorModeValue('menu-bg', 'white', chakraColor('gray.900'))
  const [ menuColor, menuColorCss ] = useColorModeValue('menu-color', 'black', 'white')
  
  return useBreakpointValue({
    base: (
      <>
        <ColorModeStyles styles={[menuBgCss, menuColorCss]} />
        <Flex onClick={onOpen} cursor={'pointer'}>
          <HamburgerIcon paddingBottom={1} boxSize={6} />
          <Heading size={'sm'} paddingLeft={4}>Table of Contents</Heading>
        </Flex>
        <Drawer isOpen={isOpen} onClose={onClose} size={'xs'} isFullHeight={true}>
          <DrawerOverlay height={'100%'} />
          <DrawerContent height={'100%'} maxHeight={'100%'} color={menuColor} backgroundColor={menuBg}>
            <DrawerBody>
              <TableOfContents categories={categories} onClick={onClose} />
            </DrawerBody>
          </DrawerContent>
        </Drawer>
      </>
    ),
    md: <TableOfContents categories={categories} />
  })
}

const GuideView = ({ category, selected }) => {
  const { id, title, content } = selected
  const guideUrl = `/guide/${id}`

  return (
    <Box>
      <Breadcrumb
        spacing="8px"
        separator={<ChevronRightIcon color="gray.500" />}
      >
        <BreadcrumbItem>
          <BreadcrumbLink href="/guide/about">Guide</BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbItem isCurrentPage>
          <BreadcrumbLink href={guideUrl}>{title}</BreadcrumbLink>
        </BreadcrumbItem>
      </Breadcrumb>
      <Box paddingY="4" paddingTop={6}>
        <Heading as="h1">
          <Link href={guideUrl}>{title}</Link>
        </Heading>
      </Box>
      <MDXRemote maxWidth={"10vw"} components={MDX} {...content} />
      <Divider mt='16' mb='4' borderColor={'gray.600'} />
      <Box align="center" fontSize='sm'>
        <Text>
          Did you found a misleading and deprecated content, or maybe you just feel this section misses important elements?
        </Text>
        <LinkBox as='article' color={'purple.400'}>
          <Flex justifyContent='center'>
            <EditIcon marginTop='1' />
            <Text marginLeft='2'>
              <LinkOverlay href={`https://github.com/dzikoysk/reposilite/blob/main/reposilite-site-next/data/guides/${category}/${id}.md`}>
                Edit this page on GitHub
              </LinkOverlay>
            </Text>
          </Flex>
        </LinkBox>
      </Box>
    </Box>
  )
}

export default function Guide({ categories, category, selected }) {
  return (
    <Layout>
      <Head>
        <title>{selected.title} · Guide · Reposilite</title>
      </Head>
      <Box
        maxW={{ base: '100%', lg: "container.lg", xl: "container.xl" }}
        mx={'auto'}
        px={6}
      >
        <Flex direction={{ base: "column", md: "row" }}>
          <Box
            paddingTop={{ base: 12, md: 24 }}
            mx={{ md: "auto" }}
            paddingLeft={{ base: 0, md: 6 }}
            paddingRight={{ base: 6, md: 16 }}
          >
            <GuideMenu categories={categories} />
          </Box>
          <Box
            maxW={{ base: "100%", md: "70%" }}
            minW={{ md: "70%" }}
            mx="auto"
            paddingTop="10"
            paddingRight={{ base: 0, md: 6 }}
          >
            <GuideView category={category} selected={selected} />
          </Box>
        </Flex>
      </Box>
    </Layout>
  )
}

export async function getStaticProps({ params: { id } }) {
  const categories = await getGuideCategories()
  const selectedCategory = categories.find((category) =>
    category.content.find((guide) => guide.id == id)
  )
  const selectedGuide = await readGuideById(selectedCategory.directory, id)

  return {
    props: {
      categories,
      category: selectedCategory.directory,
      selected: selectedGuide,
    },
  }
}

export async function getStaticPaths() {
  const categories = await getGuideCategories()

  return {
    paths: categories
      .flatMap((category) => category.content)
      .map((guide) => ({
        params: {
          id: guide.id,
        },
      })),
    fallback: false,
  }
}

