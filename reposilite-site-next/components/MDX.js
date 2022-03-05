import { Code, Heading, Link, Table, Tbody, Td, Text, Th, Thead, Tr } from "@chakra-ui/react";

export default {
  a: (props) => <Link {...props} />,
  p: (props) => <Text {...props} />,
  h1: (props) => <Heading as='h1' size={'xl'} {...props} />,
  h2: (props) => <Heading as='h2' size={'lg'} {...props} />,
  h3: (props) => <Heading as='h3' size={'md'} {...props} />,
  h4: (props) => <Heading as='h4' size={'sm'} {...props} />,
  h5: (props) => <Heading as='h5' size={'xs'} {...props} />,
  h6: (props) => <Heading as='h6' size={'xs'} {...props} />,
  pre: (props) => <Code wordBreak={'break-all'} whiteSpace={'pre'} {...props} />,
  table: (props) => <Table variant={'simple'} size={'sm'} colorScheme={'purple'} {...props} />,
  thead: (props) => <Thead {...props} />,
  tbody: (props) => <Tbody {...props} />,
  tr: (props) => <Tr {...props} />,
  th: (props) => <Th {...props} />,
  td: (props) => <Td {...props} />,
}