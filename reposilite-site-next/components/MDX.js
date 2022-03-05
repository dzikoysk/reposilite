import { Code, Heading, Link, ListItem, Table, Tbody, Td, Text, Th, Thead, Tr, UnorderedList } from "@chakra-ui/react";

export default {
  a: (props) => <Link color={'purple.500'} {...props} />,
  p: (props) => <Text paddingY={'2'} {...props} />,
  h1: (props) => <Heading as='h1' size={'xl'} paddingY={'2'} {...props} />,
  h2: (props) => <Heading as='h2' size={'lg'} paddingY={'2'} {...props} />,
  h3: (props) => <Heading as='h3' size={'md'} paddingY={'2'} {...props} />,
  h4: (props) => <Heading as='h4' size={'sm'} paddingY={'2'} {...props} />,
  h5: (props) => <Heading as='h5' size={'xs'} paddingY={'2'} {...props} />,
  pre: (props) => (
    <Code
      wordBreak={'break-all'}
      whiteSpace={'pre'}
      width='full'
      size={'sm'}
      marginY='4'
      padding='3'
      border='1px solid lightgray'
      {...props}
    />
  ),
  code: (props) => <Code wordBreak={'break-all'} whiteSpace={'pre'} {...props} />,
  table: (props) => (
    <Table
      variant={'simple'}
      size={'sm'}
      colorScheme={'purple'}
      marginY='5'
      {...props}
    />
  ),
  thead: (props) => <Thead {...props} />,
  tbody: (props) => <Tbody {...props} />,
  tr: (props) => <Tr {...props} />,
  th: (props) => <Th {...props} />,
  td: (props) => <Td {...props} />,
  ul: (props) => <UnorderedList paddingY={'2'} {...props} />,
  li: (props) => <ListItem paddingY={'0.5'} {...props} />,
}