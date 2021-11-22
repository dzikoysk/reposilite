package com.reposilite.badge

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.web.http.ErrorResponse
import org.intellij.lang.annotations.Language
import panda.std.Result
import panda.std.reactive.Reference

class BadgeFacade(
    private val repositoryId: Reference<out String>,
    private val mavenFacade: MavenFacade
) {

    /**
     * Badges use non-monospaced font, so we need to trim short chars to estimate real width of text
     */
    private val shortCharacters = listOf('i', 'I', 'f', 'j', 'l', '.', '-', '1')

    private fun String.countShortCharacters(): Int =
        shortCharacters.sumOf { this.count { char -> char == it } }

    fun findLatestBadge(repository: String, gav: String): Result<String, ErrorResponse> =
        mavenFacade.findLatest(LookupRequest(null, repository, gav))
            .map { version ->
                val padding = 11
                val textPadding = 110

                val versionShortCharacters = version.countShortCharacters()
                val versionWidth = version.length * 6 - versionShortCharacters * 1 + (2 * padding)
                val versionTextLength = version.length * 60 - versionShortCharacters * 10

                val id = repositoryId.get()
                val idShortCharacters = id.countShortCharacters()
                val idWidth = id.length * 6 - idShortCharacters * 1 + (2 * padding)
                val idTextLength = id.length * 60 - idShortCharacters * 10

                val fullWidth = versionWidth + idWidth

                @Language("xml")
                val badge = """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$fullWidth" height="20" role="img" aria-label="$id: $version">
                    <title>$id: $version</title>
                    <linearGradient id="s" x2="0" y2="100%">
                        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/><stop offset="1" stop-opacity=".1"/>
                    </linearGradient>      
                    <clipPath id="r"> 
                        <rect width="$fullWidth" height="20" rx="3" fill="#fff"/>
                    </clipPath>
                    <g clip-path="url(#r)">
                        <rect width="$idWidth" height="20" fill="#555"/>
                        <rect x="$idWidth" width="$versionWidth" height="20" fill="#007ec6"/>
                        <rect width="$fullWidth" height="20" fill="url(#s)"/>
                    </g>
                    <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="110"> 
                        <text aria-hidden="true" x="${textPadding + idTextLength / 2}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$idTextLength">
                            $id
                        </text>
                        <text x="${textPadding + idTextLength / 2}" y="140" transform="scale(.1)" fill="#fff" textLength="$idTextLength">
                            $id
                        </text>
                        <text aria-hidden="true" x="${idTextLength + (versionTextLength / 2) + 3 * textPadding}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$versionTextLength">
                            $version
                        </text>
                        <text x="${idTextLength + (versionTextLength / 2) + 3 * textPadding}" y="140" transform="scale(.1)" fill="#fff" textLength="$versionTextLength">
                            $version
                        </text>
                    </g>
                </svg>
                """

                badge
            }

}