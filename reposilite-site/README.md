# Reposilite - Site

Project website hosted through GitHub Pages attached to [docs](https://github.com/dzikoysk/reposilite/tree/main/docs) directory:

* https://reposilite.com

Sources are built on top of the [Docusaurus v1](https://v1.docusaurus.io/). Module structure:

```
docs        -  content in markdown format used by documentation generator
website     -  page sources based on Docusaurus
publish.sh  -  builds site to /docs directory and adds all modified files to Git
```

To run project locally go to `./website` directory, run `npm i` to install required dependencies and then use one of the following commands:

```bash
$ cd ./website
$ npm i
$ npm run start
```
