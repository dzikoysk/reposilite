/*
 * Copyright (c) 2021 dzikoysk
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

const React = require('react')
const CompLibrary = require('../../core/CompLibrary.js')
const Container = CompLibrary.Container
const GridBlock = CompLibrary.GridBlock

function Help(props) {
  const {config: siteConfig, language = ''} = props
  const {baseUrl, docsUrl} = siteConfig
  const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`
  const langPart = `${language ? `${language}/` : ''}`
  const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`

  const helpLinks = [
    {
      title: 'Visit Issues',
      content: 'Ask questions about the documentation and project in [Reposilite Issues](https://github.com/dzikoysk/reposilite/issues).'
    },
    {
      title: 'Join Discord',
      content: `Use Discord to join our friendly [Reposilite community](https://discord.gg/qGRqmGjUFX) where you can directly ask your question.`
    },
    {
      title: 'Browse Guide',
      content: `Learn more about Reposilite using the [official guide on this site.](${docUrl('about')}) Also, take a look at FAQ section.`
    },
    {
      title: 'Stay up to date',
      content: "Remember to use the latest version of Reposilite!",
    }
  ]

  const supportLinks = [
    {
      title: 'Star 🌟',
      content: "Star project on [GitHub](https://github.com/dzikoysk/reposilite/stargazers), so it'll be easier for others to find Reposilite in the future."
    },
    {
      title: 'Contribute ✒️',
      content: "Contribute to project on GitHub, or just join Discord and help others with Reposilite.",
    },
    {
      title: 'Donate 🎁',
      content: `Support project financially through [GitHub Sponsors program or through panda-lang organization](${docUrl("about#support-%EF%B8%8F")})!`,
    }
  ]

  return (
    <div className="docMainWrapper wrapper">
      <Container className="mainContainer documentContainer postContainer">
        <div className="post">
          <header className="postHeader">
            <h1>I need help :&lt;</h1>
          </header>
          <GridBlock contents={helpLinks} layout="fourColumn" />
          <header className="postHeader">
            <h1>I want to help!</h1>
          </header>
          <p>
            This project is mainly maintained by 
            <a href="https://github.com/dzikoysk"> @dzikoysk</a>. 
            To support this initiative, visit:
          </p>
          <GridBlock contents={supportLinks} layout="fourColumn" />
        </div>
      </Container>
    </div>
  );
}

module.exports = Help;
