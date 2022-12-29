package com.reposilite.frontend

import org.intellij.lang.annotations.Language

object NotFoundTemplate {

    private val uriFormatter = Regex("/+") // exclude common typos from URI
    private val regexAntiXss = Regex("[^A-Za-z0-9/.\\- ]") // exclude custom non-standard characters from template

    fun createNotFoundPage(basePath: String, originUri: String, details: String): String {
        val uri = originUri.replace(uriFormatter, "/")
        val dashboardUrl = basePath + (if (basePath.endsWith("/")) "" else "/") + "#" + uri

        @Language("html")
        val response = """
        <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <title>Reposilite - 404 Not Found</title>
            </head>
            <style>
              body {
                height: calc(100vh - 170px);
                display: flex;
                justify-content: center;
                align-items: center;
                font-family: Arial, Helvetica, sans-serif;
              }
            
              .error-view {
                text-align: center;
                width: 100vh;
                height: 100px;
              }
            
              .spooky p {
                margin-top: 0;
                margin-bottom: 0;
                font-size: 1.2rem;
                font-weight: lighter;
              }
              
              a:link, a:visited {
                color: rebeccapurple;
              }
            </style>
            <body>
              <div class='error-view'>
                <h1 style="font-size: 1.5rem">
                  <span style="color: gray;">404︱</span>Resource not found
                </h1>
                ${if (details.isEmpty()) "" else "<p><i>${regexAntiXss.replace(details, "")}</i></p>" }
                <p>Looking for a dashboard?</p>
                <div class="spooky">
                  <p>{\__/}</p>
                  <p>(●ᴗ●)</p>
                  <p>( >🥕</p>
                </div>
                <p>Visit <a href="$dashboardUrl" style="color: rebeccapurple; text-decoration: none;">$dashboardUrl</a></p>
              </div>
            </body>
        </html>
        """

        return response.trimIndent()
    }

}