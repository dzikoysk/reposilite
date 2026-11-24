/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Avatar, Box, Flex, Grid, Heading, Image, Link, SimpleGrid, Text, Tooltip, VisuallyHidden } from "@chakra-ui/react"
import { ColorModeStyles, useColorModeValue } from "nextjs-color-mode"
import releaseSponsorGroups from "../../../reposilite-frontend/src/data/version-sponsors.json"
import { chakraColor } from "../../helpers/chakra-theme"

const [leadSponsors, ...supporterTiers] = releaseSponsorGroups.map(group => group.filter(sponsor => sponsor !== 'rdehuyss'))
const sponsorHandles = { 'Jan Bojarczuk': 'Kebab81' }
const sponsorNames = { talismanplatform: 'Talisman Platform' }

function JobRunrWordmark() {
  return (
    <Flex w='12rem' maxW='full' h='2.75rem' align='center' justify='center'>
      <Image
        src='/images/sponsors/jobrunr.svg'
        alt='JobRunr'
        w='full'
        sx={{ '.next-dark-theme &': { display: 'none' } }}
      />
      <Image
        src='/images/sponsors/jobrunr-white.png'
        alt='JobRunr'
        w='90%'
        display='none'
        sx={{ '.next-dark-theme &': { display: 'block' } }}
      />
    </Flex>
  )
}

function SponsorLink({ sponsor, tier }) {
  const handle = sponsorHandles[sponsor] ?? sponsor
  const name = sponsorNames[sponsor] ?? sponsor
  const showName = tier <= 2
  const link = (
    <Link
      href={`https://github.com/${handle}`}
      aria-label={showName ? undefined : name}
      display='inline-flex'
      alignItems='center'
      maxW='full'
      gap={1}
      p={showName ? 0 : 0.5}
      borderRadius={showName ? 'md' : 'full'}
      _hover={showName ? { textDecoration: 'underline' } : { boxShadow: '0 0 0 1px var(--chakra-colors-purple-400)' }}
      _focusVisible={{ outline: '2px solid', outlineColor: 'purple.400', outlineOffset: '2px' }}
    >
      {tier === 1 && sponsor === 'JobRunr' ? <JobRunrWordmark /> : (
        <>
          <Avatar
            size='xs'
            boxSize={6}
            name={name}
            src={`https://github.com/${handle}.png?size=96`}
            aria-hidden='true'
            bg='purple.100'
            color='purple.800'
          />
          {showName && <Text fontSize='sm' fontWeight='semibold'>{name}</Text>}
        </>
      )}
    </Link>
  )

  return showName ? link : <Tooltip label={name}>{link}</Tooltip>
}

export default function Sponsors() {
  const [background, backgroundCss] = useColorModeValue('sponsor-bg', chakraColor('gray.50'), chakraColor('gray.900'))
  const [divider, dividerCss] = useColorModeValue('sponsor-divider', chakraColor('gray.300'), chakraColor('gray.600'))
  const [tierColor, tierColorCss] = useColorModeValue('sponsor-tier-color', chakraColor('gray.600'), chakraColor('gray.300'))
  const [leadColor, leadColorCss] = useColorModeValue('sponsor-lead-color', chakraColor('purple.700'), chakraColor('purple.200'))

  return (
    <Box
      as='section'
      aria-labelledby='release-sponsors-heading'
      maxW='calc(100vw - 2rem)'
      mb={8}
      p={{ base: 4, md: 7 }}
      borderRadius='2xl'
      backgroundColor={background}
    >
      <ColorModeStyles styles={[backgroundCss, dividerCss, tierColorCss, leadColorCss]} />
      <VisuallyHidden as='h2' id='release-sponsors-heading'>Release sponsors</VisuallyHidden>
      <Grid
        templateColumns={{ base: 'minmax(0, 1fr)', sm: 'minmax(0, 1fr) minmax(0, 1.7fr)' }}
        gap={{ base: 3, sm: 5 }}
      >
        <Flex
          direction='column'
          align={{ base: 'start', sm: 'center' }}
          justify='center'
          gap={2}
          pb={{ base: 3, sm: 0 }}
          borderBottomWidth={{ base: '1px', sm: 0 }}
          borderInlineEndWidth={{ base: 0, sm: '1px' }}
          borderColor={divider}
        >
          <Heading as='h3' size='xs' color={leadColor}>Tier 1 · Lead sponsor</Heading>
          {leadSponsors.map(sponsor => <SponsorLink key={sponsor} sponsor={sponsor} tier={1} />)}
        </Flex>
        <SimpleGrid columns={2} spacing={3}>
          {supporterTiers.map((group, index) => (
            <Box key={group[0]} minW={0}>
              <Heading as='h3' size='xs' mb={1} color={tierColor}>Tier {index + 2}</Heading>
              <Flex wrap='wrap' gap={1}>
                {group.map(sponsor => <SponsorLink key={sponsor} sponsor={sponsor} tier={index + 2} />)}
              </Flex>
            </Box>
          ))}
        </SimpleGrid>
      </Grid>
    </Box>
  )
}
