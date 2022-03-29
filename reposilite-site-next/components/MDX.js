import {
  Box,
  Code,
  Heading,
  Image,
  ListItem,
  OrderedList,
  Tab,
  Table,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
  UnorderedList
} from "@chakra-ui/react"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import { chakraColor } from "../helpers/chakra-theme"
import { Link } from './Link'
import Spoiler from './Spoiler'

function getAnchor(text) {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9 ]/g, '')
    .replace(/[ ]/g, '-')
}

const H = ({ children, as, size }) => {
  const anchor = getAnchor(children)

  return (
    <Heading id={anchor} as={as} size={size} paddingY={'2'}>
      <Link className="paragraph" href={`#${anchor}`} marginLeft={-4}>
        <Text as='abbr' opacity="0" color={'gray.300'} fontWeight='normal'>§ </Text>
        {children}
      </Link>
    </Heading>
  )
}

const CodeVariant = ({ children, name }) => {
  return (
    <Box name={name}>
      {children}
    </Box>
  )
}

const CodeVariants = ({ children }) => {
  const [ bg, bgCss ] = useColorModeValue('code-variant-bg', chakraColor('gray.50'), chakraColor('gray.700'))

  return (
    <>
      <ColorModeStyles styles={[bgCss]} />
      <Tabs
        variant='enclosed'
        colorScheme={''}
        marginTop={4}
        marginBottom={4}
        backgroundColor={bg}
        borderRadius={'lg'}
      >
        <TabList>
          {children.map(({ props }) => (
            <Tab key={props.name}>{props.name}</Tab>
          ))}
        </TabList>
        <TabPanels>
          {children.map(variant => (
            <TabPanel key={variant.props.name} paddingY={2} paddingX={6}>
              {variant}
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </>
  )
}

// `text`
const Highlight = (props) => (
  <Code
    fontSize={'xs'}
    background='purple.100'
    borderRadius={'full'}
    whiteSpace={'pre'}
    wordSpacing={'normal'}
    wordBreak={'normal'}
    paddingX='2'
    {...props}
  />
)

// ```text```
const Snippet = (props) => (
  <Box
    border="1px solid black"
    background={'#282a36'}
    marginY='4'
    overflow='x-auto'
    borderRadius='md'
  >
    <pre {...props} />
  </Box>
)

const MdTable = (props) => {
  const [borderColor, borderColorCss] = useColorModeValue('table-border-color', chakraColor('gray.200'), chakraColor('gray.600'))
  
  return (
    <>
      <ColorModeStyles styles={[borderColorCss]} />
      <Box
        paddingX='0'
        paddingY='0'
        border='1px'
        borderColor={borderColor}
        borderRadius='lg'
        marginY='4'
        maxWidth={'100vw'}
        overflow="auto"
      >
        <Table
          variant={'simple'}
          size={'sm'}
          {...props}
        />
      </Box>
    </>
  )
}

const TableHeader = (props) => {
  const [color, colorCss] = useColorModeValue('th-color', chakraColor('gray.500'), chakraColor('gray.300'))

  return (
    <>
      <ColorModeStyles styles={[colorCss]} />
      <Th py={3} px={5} color={color} border={'none'} {...props} />
    </>
  )
}

const TableRow = (props) =>
  <Tr borderRadius='lg' border={'none'} {...props} />

const TableCell = (props) => {
  const [borderColor, borderColorCss] = useColorModeValue('td-border-color', chakraColor('gray.200'), chakraColor('gray.600'))

  return (
    <>
      <ColorModeStyles styles={[borderColorCss]} />
      <Td
        py={2}
        px={{ base: 3, xl: 6 }}
        borderBottom={'none'}
        borderTop={'1px'}
        borderTopColor={borderColor}
        wordBreak={'break-word'}
        {...props}
      />
    </>
  )
}

export default {
  a: (props) => <Link color={'purple.400'} {...props} />,
  p: (props) => <Text paddingY={'2'} {...props} />,
  h1: (props) => <H as='h1' size={'xl'} {...props} />,
  h2: (props) => <H as='h2' size={'lg'} {...props} />,
  h3: (props) => <H as='h3' size={'md'} {...props} />,
  h4: (props) => <H as='h4' size={'sm'} {...props} />,
  h5: (props) => <H as='h5' size={'xs'} {...props} />,
  pre: Snippet,
  code: Highlight,
  table: MdTable,
  thead: (props) => <Thead {...props} />,
  tbody: (props) => <Tbody {...props} />,
  tr: TableRow,
  th: TableHeader,
  td: TableCell,
  ul: (props) => <UnorderedList paddingY={2} {...props} />,
  ol: (props) => <OrderedList paddingY={2} {...props} />,
  li: (props) => <ListItem paddingY={0.5} {...props} />,
  img: (props) =>  <Image display={'inline-block'} {...props} marginBottom={-1} />,
  Spoiler: (props) => <Spoiler {...props} />,
  CodeVariants: (props) => <CodeVariants {...props} />,
  CodeVariant: (props) => <CodeVariant {...props} />,
  Snippet,
  Highlight
}
