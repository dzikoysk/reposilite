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