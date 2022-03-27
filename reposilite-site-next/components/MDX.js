import {
  Box,
  Code,
  Heading,
  Image,
  ListItem,
  OrderedList,
  Table,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
  UnorderedList
} from "@chakra-ui/react"
import { Link } from './Link'
import Spoiler from './Spoiler'

// todo: add dark/light switch on top of the next-color-mode library

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

export default {
  a: (props) => <Link color={'purple.400'} {...props} />,
  p: (props) => <Text paddingY={'2'} {...props} />,
  h1: (props) => <H as='h1' size={'xl'} {...props} />,
  h2: (props) => <H as='h2' size={'lg'} {...props} />,
  h3: (props) => <H as='h3' size={'md'} {...props} />,
  h4: (props) => <H as='h4' size={'sm'} {...props} />,
  h5: (props) => <H as='h5' size={'xs'} {...props} />,
  /// ```text```
  pre: (props) => (
    <Box
      border="1px solid black"
      background={'#282a36'}
      marginY='4'
      overflow='x-auto'
      borderRadius='md'
    >
      <pre {...props} />
    </Box>
  ),
  // `text`
  code: (props) => (
    <Code
      fontSize={'xs'}
      maxWidth={'full'}
      background='purple.100'
      borderRadius={'full'}
      whiteSpace={'pre'}
      wordSpacing={'normal'}
      wordBreak={'normal'}
      paddingX='2'
      {...props}
    />
  ),
  table: (props) => (
    <Box
      paddingX='0'
      paddingY='0'
      border='1px'
      borderColor={'gray.200'}
      borderRadius='lg'
      marginY='4'
    >
      <Table
        variant={'simple'}
        size={'sm'}
        {...props}
      />
    </Box>
  ),
  thead: (props) => <Thead {...props} />,
  tbody: (props) => <Tbody {...props} />,
  tr: (props) => <Tr borderRadius='lg'{...props} />,
  th: (props) => <Th py={3} px={5} color={'gray.500'} {...props} />,
  td: (props) => <Td py={2} px={5} wordBreak={'break-all'} {...props} />,
  ul: (props) => <UnorderedList paddingY={2} {...props} />,
  ol: (props) => <OrderedList paddingY={2} {...props} />,
  li: (props) => <ListItem paddingY={0.5} {...props} />,
  img: (props) =>  <Image display={'inline-block'} {...props} marginBottom={-1} />,
  Spoiler: (props) => <Spoiler {...props} />
}
