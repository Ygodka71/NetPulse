package com.example.model

enum class MetricModuleId(
    val titleRu: String,
    val hasHelp: Boolean = false
) {
    CURRENT_PING("Пинг", hasHelp = false),
    AVG_LATENCY("Ср. задержка", hasHelp = false),
    JITTER("Джиттер", hasHelp = true),
    PACKET_LOSS("Потери\nпакетов", hasHelp = true),
    DOWNLOAD("Скачивание", hasHelp = false),
    UPLOAD("Отдача", hasHelp = false),
    GEO_IP("Внешний IP", hasHelp = true),
    CONNECTION_TYPE("Тип сети", hasHelp = true)
}

data class MetricModuleItem(
    val id: MetricModuleId,
    val isVisible: Boolean = true
)

data class MetricHelpInfo(
    val title: String,
    val description: String,
    val impactText: String,
    val goodRange: String
)

object MetricHelpDirectory {
    fun getHelpFor(id: MetricModuleId): MetricHelpInfo? {
        return when (id) {
            MetricModuleId.JITTER -> MetricHelpInfo(
                title = "Джиттер (Jitter)",
                description = "Колебание (нестабильность) времени отклика между последовательными пакетами. Показывает, насколько сильно 'скачет' задержка.",
                impactText = "Критичен для онлайн-игр (вызывает микрофризы и телепортацию), а также голосовых звонков (речь заикается, слова выпадают).",
                goodRange = "• До 5 мс — отлично\n• 5–15 мс — хорошо\n• Свыше 30 мс — высокая нестабильность сети"
            )
            MetricModuleId.PACKET_LOSS -> MetricHelpInfo(
                title = "Потери пакетов (Packet Loss)",
                description = "Процент сетевых пакетов, которые не дошли до сервера или были отброшены из-за перегрузки или помех.",
                impactText = "Даже 1–2% потерь вызывают пропадание фраз при созвонах, дисконнекты в играх и подвисания веб-страниц.",
                goodRange = "• 0% — норма\n• 1–3% — заметные помехи в радиоэфире\n• > 5% — сильная деградация канала"
            )
            MetricModuleId.GEO_IP -> MetricHelpInfo(
                title = "Внешний IP",
                description = "Уникальный сетевой адрес, под которым ваши устройства видны в глобальном интернете. Определяется через Anycast-маршрутизацию провайдера.",
                impactText = "Позволяет проверить, не включен ли VPN или прокси, и через какой узел/страну идет ваш трафик к целевым серверам.",
                goodRange = "Обычно совпадает с вашей реальной страной и оператором связи."
            )
            MetricModuleId.CONNECTION_TYPE -> MetricHelpInfo(
                title = "Тип сети",
                description = "Технология связи (Wi-Fi, 4G LTE, Ethernet) и физическая пропускная способность линка между вашим телефоном и роутером/сотовой вышкой.",
                impactText = "Показывает ограничение на физическом уровне радиоканала. Реальная скорость интернета не может превышать этот линк.",
                goodRange = "Wi-Fi 5 ГГц: 300–866+ Мбит/с\nWi-Fi 2.4 ГГц: 72–144 Мбит/с\n4G LTE: 50–150 Мбит/с"
            )
            else -> null
        }
    }
}
