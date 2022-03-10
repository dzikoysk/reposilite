import { Box, Code, Heading, Link, ListItem, Table, Tbody, Td, Text, Th, Thead, Tr, UnorderedList } from "@chakra-ui/react";

// todo: add dark/light switch on top of the next-color-mode library

export default {
  a: (props) => <Link color={'purple.400'} {...props} />,
  p: (props) => <Text paddingY={'2'} {...props} />,
  h1: (props) => <Heading as='h1' size={'xl'} paddingY={'2'} {...props} />,
  h2: (props) => <Heading as='h2' size={'lg'} paddingY={'2'} {...props} />,
  h3: (props) => <Heading as='h3' size={'md'} paddingY={'2'} {...props} />,
  h4: (props) => <Heading as='h4' size={'sm'} paddingY={'2'} {...props} />,
  h5: (props) => <Heading as='h5' size={'xs'} paddingY={'2'} {...props} />,
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
      paddingX='4'
      paddingY='2'
      border='1px'
      borderColor={'gray.200'}
      borderRadius='lg'
      marginY='4'
    >
      <Table
        variant={'simple'}
        size={'xs'}
        {...props}
      />
    </Box>
  ),
  thead: (props) => <Thead {...props} />,
  tbody: (props) => <Tbody {...props} />,
  tr: (props) => <Tr {...props} />,
  th: (props) => <Th {...props} />,
  td: (props) => <Td {...props} />,
  ul: (props) => <UnorderedList paddingY={'2'} {...props} />,
  li: (props) => <ListItem paddingY={'0.5'} {...props} />,
}