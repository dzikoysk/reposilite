package com.reposilite.shared

import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.BAD_REQUEST
import org.intellij.lang.annotations.Language
import panda.std.Result
import panda.std.asSuccess

object BadgeGenerator {

    /**
     * Just in case, mostly to avoid issues with XML based template
     */
    private val supportedCharacters = Regex("[\\sa-zA-Z0-9:\\-.,+=()\\[\\]]+")
    /**
     * Badges use non-monospaced font, so we need to trim short chars to estimate real width of text
     */
    private val shortCharacters = listOf('i', 'I', 'f', 'j', 'l', '.', '-', '1')
    /**
     * Standard blue color used by well-known badges on GitHub
     */
    private const val colorBlue = "007ec6"
    /**
     * Standard green color used by well-known badges on GitHub
     */
    private const val colorGreen = "4c1"

    private fun String.countShortCharacters(): Int =
        shortCharacters.sumOf { this.count { char -> char == it } }

    fun generateSvg(name: String, value: String, optionalColor: String?): Result<String, ErrorResponse> {
        val color = optionalColor ?: colorBlue

        if (!(name + value + color).matches(supportedCharacters)) {
            return errorResponse(BAD_REQUEST, "Request contains invalid characters")
        }

        val padding = 11
        val textPadding = 110

        val nameShortCharacters = name.countShortCharacters()
        val nameWidth = name.length * 6 - nameShortCharacters * 1 + 2 * padding
        val nameTextLength = name.length * 60 - nameShortCharacters * 10

        val valueShortCharacters = value.countShortCharacters()
        val valueWidth = value.length * 6 - valueShortCharacters * 1 + 2 * padding
        val valueTextLength = value.length * 60 - valueShortCharacters * 10

        val fullWidth = valueWidth + nameWidth

        @Language("xml")
        val badge = """
                <svg xmlns="http://www.w3.org/2000/svg" width="$fullWidth" height="20" role="img" aria-label="$name: $value">
                    <title>$name: $value</title>
                    <linearGradient id="s" x2="0" y2="100%">
                        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/><stop offset="1" stop-opacity=".1"/>
                    </linearGradient>      
                    <clipPath id="r"> 
                        <rect width="$fullWidth" height="20" rx="3" fill="#fff"/>
                    </clipPath>
                    <g clip-path="url(#r)">
                        <rect width="$nameWidth" height="20" fill="#555"/>
                        <rect x="$nameWidth" width="$valueWidth" height="20" fill="#$optionalColor"/>
                        <rect width="$fullWidth" height="20" fill="url(#s)"/>
                    </g>
                    <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="110"> 
                        <text x="${textPadding + nameTextLength / 2}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$nameTextLength">
                            $name
                        </text>
                        <text x="${textPadding + nameTextLength / 2}" y="140" transform="scale(.1)" fill="#fff" textLength="$nameTextLength">
                            $name
                        </text>
                        <text x="${nameTextLength + valueTextLength / 2 + 3 * textPadding}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$valueTextLength">
                            $value
                        </text>
                        <text x="${nameTextLength + valueTextLength / 2 + 3 * textPadding}" y="140" transform="scale(.1)" fill="#fff" textLength="$valueTextLength">
                            $value
                        </text>
                    </g>
                </svg>
                """

        return badge.asSuccess()
    }

}