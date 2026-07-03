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
        val catchupSource: String? = null,
        val country: String = "Other"
    )

    fun detectCountry(name: String, group: String, tvgCountry: String?): String {
        if (!tvgCountry.isNullOrEmpty()) {
            val tc = tvgCountry.uppercase().trim()
            if (tc.length <= 3) return tc
            // Map common country names to 2-letter codes
            when {
                tc.contains("UNITED STATES") || tc.contains("USA") -> return "US"
                tc.contains("UNITED KINGDOM") || tc.contains("UK") || tc.contains("GREAT BRITAIN") -> return "UK"
                tc.contains("CANADA") -> return "CA"
                tc.contains("FRANCE") -> return "FR"
                tc.contains("GERMANY") || tc.contains("DEUTSCH") -> return "DE"
                tc.contains("SPAIN") || tc.contains("ESPAN") -> return "ES"
                tc.contains("ITALY") || tc.contains("ITALIA") -> return "IT"
                tc.contains("MEXICO") -> return "MX"
                tc.contains("BRAZIL") || tc.contains("BRASIL") -> return "BR"
                tc.contains("NETHERLANDS") || tc.contains("HOLLAND") -> return "NL"
                tc.contains("INDIA") -> return "IN"
            }
            return tc
        }
        
        val upperName = name.uppercase()
        val upperGroup = group.uppercase()
        
        val countries = mapOf(
            "US" to listOf("USA", "US:", "US |", "[US]", "UNITED STATES", "UNITEDSTATES"),
            "UK" to listOf("UK:", "UK |", "[UK]", "UNITED KINGDOM", "UNITEDKINGDOM", "GREAT BRITAIN"),
            "CA" to listOf("CA:", "CA |", "[CA]", "CANADA"),
            "FR" to listOf("FR:", "FR |", "[FR]", "FRANCE"),
            "DE" to listOf("DE:", "DE |", "[DE]", "GERMANY", "DEUTSCH"),
            "ES" to listOf("ES:", "ES |", "[ES]", "SPAIN", "ESPAN"),
            "IT" to listOf("IT:", "IT |", "[IT]", "ITALY", "ITALIA"),
            "MX" to listOf("MX:", "MX |", "[MX]", "MEXICO"),
            "BR" to listOf("BR:", "BR |", "[BR]", "BRAZIL", "BRASIL"),
            "NL" to listOf("NL:", "NL |", "[NL]", "NETHERLANDS", "HOLLAND"),
            "IN" to listOf("IN:", "IN |", "[IN]", "INDIA")
        )
        
        for ((code, keywords) in countries) {
            for (keyword in keywords) {
                if (upperName.contains(keyword) || upperGroup.contains(keyword)) {
                    return code
                }
            }
        }
        return "Other"
    }

    fun parse(m3uContent: String, playlistId: Int): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        
        // Remove UTF-8 Byte Order Mark (BOM) if present
        val cleanContent = if (m3uContent.startsWith("\uFEFF")) {
            m3uContent.substring(1)
        } else {
            m3uContent
        }

        val reader = BufferedReader(StringReader(cleanContent))
        var line: String? = reader.readLine()

        var currentExtInf: TempExtInf? = null

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentExtInf = parseExtInfLine(trimmed)
            } else if (trimmed.startsWith("#EXTGRP:")) {
                val group = trimmed.substringAfter("#EXTGRP:").trim()
                if (currentExtInf != null && group.isNotEmpty()) {
                    val finalCountry = detectCountry(currentExtInf.name, group, currentExtInf.country)
                    currentExtInf = currentExtInf.copy(groupTitle = group, country = finalCountry)
                }
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
                            catchupSource = currentExtInf.catchupSource,
                            country = currentExtInf.country
                        )
                    )
                } else {
                    // Fallback if no #EXTINF preceded the URL
                    val nameFromUrl = trimmed.substringAfterLast("/").substringBefore("?")
                    val detectedCry = detectCountry(nameFromUrl, "Uncategorized", null)
                    channels.add(
                        ChannelEntity(
                            playlistId = playlistId,
                            name = if (nameFromUrl.isNotEmpty()) nameFromUrl else "Stream ${channels.size + 1}",
                            logoUrl = null,
                            groupTitle = "Uncategorized",
                            streamUrl = trimmed,
                            tvgId = null,
                            tvgName = null,
                            catchupType = null,
                            catchupDays = 0,
                            catchupSource = null,
                            country = detectedCry
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
        // Format is: #EXTINF:-1 tvg-id="..." tvg-logo="...",Channel Name
        val commaIndex = line.lastIndexOf(",")
        val attributesPart = if (commaIndex != -1) line.substring(0, commaIndex) else line
        val namePart = if (commaIndex != -1) line.substring(commaIndex + 1).trim() else "Unknown Channel"

        val attributes = extractAttributes(attributesPart)

        val tvgId = attributes["tvg-id"]
        val tvgLogo = attributes["tvg-logo"] ?: attributes["logo"] ?: attributes["tvg-logo-url"] ?: attributes["icon"]
        var groupTitle = attributes["group-title"] ?: attributes["group"] ?: attributes["tvg-group"]
        if (groupTitle.isNullOrEmpty()) {
            groupTitle = "Other"
        }
        val tvgName = attributes["tvg-name"]

        val catchup = attributes["catchup"]
        val catchupDaysStr = attributes["catchup-days"]
        val catchupSource = attributes["catchup-source"]

        val catchupDays = catchupDaysStr?.toIntOrNull() ?: 0
        
        val tvgCountry = attributes["tvg-country"] ?: attributes["country"] ?: attributes["tvg-country-code"]
        val detectedCry = detectCountry(namePart, groupTitle, tvgCountry)

        return TempExtInf(
            name = namePart,
            logoUrl = tvgLogo,
            groupTitle = groupTitle,
            tvgId = tvgId,
            tvgName = tvgName,
            catchupType = catchup,
            catchupDays = catchupDays,
            catchupSource = catchupSource,
            country = detectedCry
        )
    }

    private fun extractAttributes(source: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        // Highly robust Regex to extract key=value pairs (double-quoted, single-quoted, or unquoted)
        val regex = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")
        val matches = regex.findAll(source)
        for (match in matches) {
            val key = match.groups[1]?.value?.lowercase() ?: continue
            val value = match.groups[2]?.value 
                ?: match.groups[3]?.value 
                ?: match.groups[4]?.value 
                ?: ""
            map[key] = value
        }
        return map
    }
}
