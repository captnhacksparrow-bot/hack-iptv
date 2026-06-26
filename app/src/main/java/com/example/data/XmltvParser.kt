package com.example.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object XmltvParser {

    fun parse(inputStream: InputStream): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()
        val parser = Xml.newPullParser()
        try {
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var currentProgram: EpgProgramEntityBuilder? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            val channelId = parser.getAttributeValue(null, "channel") ?: ""
                            val startStr = parser.getAttributeValue(null, "start") ?: ""
                            val stopStr = parser.getAttributeValue(null, "stop") ?: ""
                            currentProgram = EpgProgramEntityBuilder(
                                channelTvgId = channelId,
                                startTime = parseXmltvDate(startStr),
                                endTime = parseXmltvDate(stopStr)
                            )
                        } else if (currentProgram != null) {
                            when (name) {
                                "title" -> {
                                    currentProgram.title = parser.nextText()
                                }
                                "desc" -> {
                                    currentProgram.description = parser.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "programme" && currentProgram != null) {
                            if (currentProgram.channelTvgId.isNotEmpty() && currentProgram.title.isNotEmpty()) {
                                programs.add(currentProgram.build())
                            }
                            currentProgram = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return programs
    }

    private fun parseXmltvDate(dateStr: String): Long {
        val cleanDate = dateStr.trim()
        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(cleanDate)?.time ?: 0L
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        return 0L
    }

    private class EpgProgramEntityBuilder(
        val channelTvgId: String,
        val startTime: Long,
        val endTime: Long,
        var title: String = "",
        var description: String? = null
    ) {
        fun build() = EpgProgramEntity(
            channelTvgId = channelTvgId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            isCatchupAvailable = endTime < System.currentTimeMillis() // if program is in the past, it's catchup!
        )
    }
}
