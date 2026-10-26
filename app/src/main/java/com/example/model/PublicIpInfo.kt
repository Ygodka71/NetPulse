package com.example.model

data class PublicIpInfo(
    val ip: String,
    val countryCode: String,
    val countryName: String,
    val flagEmoji: String,
    val isp: String = ""
) {
    fun getCountryDisplayName(isEn: Boolean): String {
        val code = countryCode.trim().uppercase()
        if (code.isEmpty() || code == "XX") {
            return countryName
        }
        return if (isEn) {
            val enName = java.util.Locale("", code).getDisplayCountry(java.util.Locale.ENGLISH)
            if (enName.isNotBlank() && !enName.equals(code, ignoreCase = true)) {
                enName
            } else {
                countryName
            }
        } else {
            getCountryNameRu(code)
        }
    }

    companion object {
        fun countryCodeToFlagEmoji(countryCode: String): String {
            if (countryCode.length != 2) return "🌐"
            val firstChar = countryCode[0].uppercaseChar()
            val secondChar = countryCode[1].uppercaseChar()
            if (firstChar !in 'A'..'Z' || secondChar !in 'A'..'Z') return "🌐"

            val firstUnicode = 0x1F1E6 + (firstChar - 'A')
            val secondUnicode = 0x1F1E6 + (secondChar - 'A')
            return String(Character.toChars(firstUnicode)) + String(Character.toChars(secondUnicode))
        }

        fun getCountryNameRu(countryCode: String): String {
            return when (countryCode.uppercase()) {
                "RU" -> "Россия"
                "KZ" -> "Казахстан"
                "BY" -> "Беларусь"
                "UA" -> "Украина"
                "UZ" -> "Узбекистан"
                "KG" -> "Кыргызстан"
                "AM" -> "Армения"
                "GE" -> "Грузия"
                "AZ" -> "Азербайджан"
                "US" -> "США"
                "DE" -> "Германия"
                "FR" -> "Франция"
                "GB", "UK" -> "Великобритания"
                "NL" -> "Нидерланды"
                "FI" -> "Финляндия"
                "SE" -> "Швеция"
                "TR" -> "Турция"
                "AE" -> "ОАЭ"
                "CN" -> "Китай"
                "JP" -> "Япония"
                "KR" -> "Южная Корея"
                "PL" -> "Польша"
                "IT" -> "Италия"
                "ES" -> "Испания"
                "CA" -> "Канада"
                "CH" -> "Швейцария"
                "AT" -> "Австрия"
                "CZ" -> "Чехия"
                "NO" -> "Норвегия"
                "DK" -> "Дания"
                "EE" -> "Эстония"
                "LV" -> "Латвия"
                "LT" -> "Литва"
                else -> {
                    val ruName = java.util.Locale("", countryCode.uppercase()).getDisplayCountry(java.util.Locale("ru"))
                    if (ruName.isNotBlank() && !ruName.equals(countryCode, ignoreCase = true)) {
                        ruName
                    } else {
                        countryCode.uppercase()
                    }
                }
            }
        }
    }
}
