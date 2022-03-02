import { Box, Container, Flex, Heading, Spacer, VStack, useColorModeValue } from "@chakra-ui/react";

export default function Footer() {
  return (
    <Box backgroundColor={useColorModeValue('gray.100', 'gray.900')}>
      <Container>
        <Flex justifyContent={'center'} paddingY={10}>
          <VStack>
            <Heading fontSize={17}>Guide</Heading>
          </VStack>
          <Spacer />
          <VStack>
            <Heading fontSize={17}>Community</Heading>
          </VStack>
        </Flex>
      </Container>
    </Box>
  )
}