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

class HomeSplash extends React.Component {
  render () {
    const { siteConfig, language = '' } = this.props
    const { baseUrl, docsUrl } = siteConfig
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`
    const langPart = `${language ? `${language}/` : ''}`
    const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`

    const SplashContainer = props => (
      <div className='homeContainer'>
        <div className='homeSplashFade'>
          <div className='wrapper homeWrapper'>{props.children}</div>
        </div>
      </div>
    )

    const Button = props => (
      <div className='pluginWrapper buttonWrapper'>
        <a
          className='button'
          style={{
            width: '250px',
            background: '#1d1d1d',
            'marginTop': '15px',
            'border': 'none',
            'borderRadius': '20px',
            'color': 'white',
            'marginLeft': '10px',
            'marginRight': '10px',
          }}
          href={props.href}
          target={props.target}
        >
          {props.children}
        </a>
      </div>
    )

    return (
      <SplashContainer>
        <div className='inner' style={{ width: '70%', margin: 'auto' }}>
          <h2 className='projectTitle' style={{ color: 'black', paddingTop: '50px', paddingBottom: '10px' }}>
            {siteConfig.title}
          </h2>
          <small>
            Lightweight and easy-to-use repository manager for Maven based artifacts in JVM ecosystem.
            This is simple, extensible and scalable self-hosted solution to replace managers like
            Nexus, Archiva or Artifactory, with reduced resources consumption. 📦
          </small>
          <img src="/img/preview.png" style={{ paddingTop: '30px', paddingBottom: '5px' }} />
          <div className='section promoSection'>
            <div className='promoRow'>
              <div className='pluginRowBlock'>
                <Button href={docUrl('about')}>Get started</Button>
                <Button href={docUrl('install')}>Download Reposilite</Button>
              </div>
            </div>
          </div>
        </div>
      </SplashContainer>
    )
  }
}

class Index extends React.Component {
  render () {
    const { config: siteConfig, language = '' } = this.props

    const Block = props => (
      <Container
        id={props.id}
        style={props.style}>
        <GridBlock
          align='center'
          contents={props.children}
          style={props.style}
          layout={props.layout}
        />
      </Container>
    )

    return (
      <div style={{ color: 'black' }}>
        <HomeSplash siteConfig={siteConfig} language={language} />
        <div className='mainContainer'>
          <div id='whatis' layout='twoColumn' style={{
            display: 'flex',
            justifyContent: 'center',
            paddingTop: '10px',
            paddingBottom: '10px',
            flexWrap: 'wrap',
            background: '#f2f2f2'
          }}>
            <div>
              <h2>~ What is Reposilite? ~</h2>
              <ul>
                <li>Repository manager for Maven artifacts</li>
                <li>Alternative to managers like Nexus, Archiva or Artifactory</li>
                <li>Fully open source project</li>
              </ul>
            </div>
            <div>
              <h2>~ Why? ~</h2>
              <ul>
                <li>Reduce usage of your resources to 16MB of RAM</li>
                <li>Covered with unit & integration tests</li>
                <li>Easy to use</li>
              </ul>
            </div>
          </div>
          <Block layout='fourColumn'>
            {[
              {
                title: 'Authorization',
                content: 'Personal access-token based authorization with configurable routes and permissions'
              },
              {
                title: 'Docker 🐋',
                content: 'Available docker images [docker.com/reposilite](https://hub.docker.com/r/dzikoysk/reposilite)',
              },
              {
                title: 'Dashboard',
                content: 'Simple dashboard with repository browser and management panel'
              },
              {
                title: 'API Endpoints',
                content: 'Exposes dedicated REST API to communicate with repository using external tools',
              },
              {
                title: 'Plugin system',
                content: 'Extend your instance with personalized extensions in Java, Kotlin or Groovy',
              },
              {
                title: 'Storage providers',
                content: 'Store artifacts locally or integrate your instance with cloud object storage like AWS S3',
              },
              {
                title: 'Proxy',
                content: 'Link other Maven repositories and redirect all traffic through your instance',
              },
              {
                title: 'And...',
                content: 'Much more, take a look at <i>Guide</i> section to learn more!',
              }
            ]}
          </Block>
        </div>
      </div>
    )
  }
}

module.exports = Index
