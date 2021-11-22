package com.reposilite.badge

import com.reposilite.badge.api.LatestBadgeRequest
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
    private val colorBlue = "007ec6"
    private val colorGreen = "4c1"

    private fun String.countShortCharacters(): Int =
        shortCharacters.sumOf { this.count { char -> char == it } }

    fun findLatestBadge(request: LatestBadgeRequest): Result<String, ErrorResponse> =
        mavenFacade.findLatest(LookupRequest(null, request.repository, request.gav))
            .map { generateSvg(request.name ?: repositoryId.get(), (request.prefix ?: "") + it, request.color ?: colorBlue) }

    private fun generateSvg(name: String, value: String, color: String): String {
        val padding = 11
        val textPadding = 110

        val nameShortCharacters = name.countShortCharacters()
        val nameWidth = name.length * 6 - nameShortCharacters * 1 + (2 * padding)
        val nameTextLength = name.length * 60 - nameShortCharacters * 10

        val valueShortCharacters = value.countShortCharacters()
        val valueWidth = value.length * 6 - valueShortCharacters * 1 + (2 * padding)
        val valueTextLength = value.length * 60 - valueShortCharacters * 10

        val fullWidth = valueWidth + nameWidth

        @Language("xml")
        val badge = """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$fullWidth" height="20" role="img" aria-label="$name: $value">
                    <title>$name: $value</title>
                    <linearGradient id="s" x2="0" y2="100%">
                        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/><stop offset="1" stop-opacity=".1"/>
                    </linearGradient>      
                    <clipPath id="r"> 
                        <rect width="$fullWidth" height="20" rx="3" fill="#fff"/>
                    </clipPath>
                    <g clip-path="url(#r)">
                        <rect width="$nameWidth" height="20" fill="#555"/>
                        <rect x="$nameWidth" width="$valueWidth" height="20" fill="#$color"/>
                        <rect width="$fullWidth" height="20" fill="url(#s)"/>
                    </g>
                    <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="110"> 
                        <text aria-hidden="true" x="${textPadding + nameTextLength / 2}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$nameTextLength">
                            $name
                        </text>
                        <text x="${textPadding + nameTextLength / 2}" y="140" transform="scale(.1)" fill="#fff" textLength="$nameTextLength">
                            $name
                        </text>
                        <text aria-hidden="true" x="${nameTextLength + (valueTextLength / 2) + 3 * textPadding}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$valueTextLength">
                            $value
                        </text>
                        <text x="${nameTextLength + (valueTextLength / 2) + 3 * textPadding}" y="140" transform="scale(.1)" fill="#fff" textLength="$valueTextLength">
                            $value
                        </text>
                    </g>
                </svg>
                """

        return badge
    }

}