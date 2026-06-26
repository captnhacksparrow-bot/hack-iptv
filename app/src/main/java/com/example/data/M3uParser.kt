package com.example.data

import java.io.BufferedReader
import java.io.StringReader

object M3uParser {

    data class TempExtInf(
        val name: String = "",
        val logoUrl: String? = null,
        val groupTitle: String = "Other",
        val tvgId: String? = null,
        val tvgName: String? = null,
        val catchupType: String? = null,
        val catchupDays: Int = 0,
        val catchupSource: String? = null
    )

    fun parse(m3uContent: String, playlistId: Int): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val reader = BufferedReader(StringReader(m3uContent))
        var line: String? = reader.readLine()

        var currentExtInf: TempExtInf? = null

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentExtInf = parseExtInfLine(trimmed)
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This line is the stream URL
                if (currentExtInf != null) {
                    channels.add(
                        ChannelEntity(
                            playlistId = playlistId,
                            name = currentExtInf.name,
                            logoUrl = currentExtInf.logoUrl,
                            groupTitle = currentExtInf.groupTitle,
                            streamUrl = trimmed,
                            tvgId = currentExtInf.tvgId,
                            tvgName = currentExtInf.tvgName,
                            catchupType = currentExtInf.catchupType,
                            catchupDays = currentExtInf.catchupDays,
                            catchupSource = currentExtInf.catchupSource
                        )
                    )
                } else {
                    // Fallback if no #EXTINF was preceding
                    val name = trimmed.substringAfterLast("/").substringBefore("?")
                    channels.add(
                        ChannelEntity(
                            playlistId = playlistId,
                            name = if (name.isNotEmpty()) name else "Stream ${channels.size + 1}",
                            logoUrl = null,
                            groupTitle = "Uncategorized",
                            streamUrl = trimmed,
                            tvgId = null,
                            tvgName = null,
                            catchupType = null,
                            catchupDays = 0,
                            catchupSource = null
                        )
                    )
                }
                currentExtInf = null
            }
            line = reader.readLine()
        }
        return channels
    }

    private fun parseExtInfLine(line: String): TempExtInf {
        // Strip out duration to parse attributes
        // Format is: #EXTINF:-1 tvg-id="..." tvg-logo="...",Channel Name
        val commaIndex = line.lastIndexOf(",")
        val attributesPart = if (commaIndex != -1) line.substring(0, commaIndex) else line
        val namePart = if (commaIndex != -1) line.substring(commaIndex + 1).trim() else "Unknown Channel"

        val tvgId = extractAttribute(attributesPart, "tvg-id")
        val tvgLogo = extractAttribute(attributesPart, "tvg-logo")
        var groupTitle = extractAttribute(attributesPart, "group-title")
        if (groupTitle.isNullOrEmpty()) {
            groupTitle = extractAttribute(attributesPart, "group") ?: "Other"
        }
        val tvgName = extractAttribute(attributesPart, "tvg-name")

        val catchup = extractAttribute(attributesPart, "catchup")
        val catchupDaysStr = extractAttribute(attributesPart, "catchup-days")
        val catchupSource = extractAttribute(attributesPart, "catchup-source")

        val catchupDays = catchupDaysStr?.toIntOrNull() ?: 0

        return TempExtInf(
            name = namePart,
            logoUrl = tvgLogo,
            groupTitle = groupTitle,
            tvgId = tvgId,
            tvgName = tvgName,
            catchupType = catchup,
            catchupDays = catchupDays,
            catchupSource = catchupSource
        )
    }

    private fun extractAttribute(source: String, attributeName: String): String? {
        val keys = listOf("$attributeName=\"", "$attributeName=")
        for (key in keys) {
            val index = source.indexOf(key)
            if (index != -1) {
                val start = index + key.length
                if (key.endsWith("\"")) {
                    val end = source.indexOf("\"", start)
                    if (end != -1) {
                        return source.substring(start, end)
                    }
                } else {
                    // Unquoted value, find next space or end
                    val end = source.indexOf(" ", start)
                    return if (end != -1) {
                        source.substring(start, end).trim()
                    } else {
                        source.substring(start).trim()
                    }
                }
            }
        }
        return null
    }
}
