/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Accordion, AccordionButton, AccordionIcon, AccordionItem, AccordionPanel, Box } from "@chakra-ui/react"

export default function Spoiler({ title, paddingX, paddingY, children }) {
  return (
    <Accordion my='4' bg='#282a36' borderRadius='xl' allowToggle>
    <AccordionItem border='0'>
      <h2>
        <AccordionButton color='purple.50'>
          <Box flex='1' textAlign='left' textDecoration={'underline'}>{title}</Box>
          <AccordionIcon />
        </AccordionButton>
      </h2>
        <AccordionPanel py={paddingY ?? 0} px={paddingX ?? 0}>
        {children}
      </AccordionPanel>
    </AccordionItem>
  </Accordion>
  )
}