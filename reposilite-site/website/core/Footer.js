/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

class Footer extends React.Component {
  docUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    const docsUrl = this.props.config.docsUrl;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    return `${baseUrl}${docsPart}${langPart}${doc}`;
  }

  pageUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    return baseUrl + (language ? `${language}/` : '') + doc;
  }

  render() {
    return (
      <footer className="nav-footer" id="footer">
        <section className="sitemap center">
          <div>
            <h5>Guide</h5>
            <a href={this.docUrl('about', this.props.language)}>
              Getting Started
            </a>
            <a href={this.docUrl('install', this.props.language)}>
              Installation
            </a>
            <a href={this.docUrl('developers', this.props.language)}>
              Developer API
            </a>
          </div>
          <div>
            <h5>Community</h5>
            <a href="https://github.com/dzikoysk/reposilite/issues" target="_blank">
              Report Issue
            </a>
            <a href="https://github.com/dzikoysk/reposilite" target="_blank">Star Reposilite</a>
            <a href="https://twitter.com/dzikoysk" target="_blank">Follow @dzikoysk</a>
          </div>
        </section>

        <section className="copyright">{this.props.config.copyright}</section>
      </footer>
    );
  }
}

module.exports = Footer;
