module.exports = {
    outputDir: '../reposilite-backend/src/main/resources/frontend/',
    filenameHashing: false,
    productionSourceMap: false,
    css: {
        extract: false
    },
    chainWebpack: config => {
        config.optimization.delete('splitChunks')
    }
}