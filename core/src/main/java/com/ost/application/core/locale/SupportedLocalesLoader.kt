package com.ost.application.core.locale
import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import java.util.Locale
object SupportedLocalesLoader {
    fun load(context: Context, localesConfigXmlResId: Int): List<Locale> {
        val locales = mutableListOf<Locale>()
        try {
            val parser: XmlResourceParser = context.resources.getXml(localesConfigXmlResId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val langTag = parser.getAttributeValue(
                        "http://schemas.android.com/apk/res/android", "name"
                    )
                    if (langTag != null) {
                        locales.add(Locale.forLanguageTag(langTag))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return locales
    }
}
