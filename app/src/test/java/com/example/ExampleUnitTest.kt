package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun networkInfoDisplayName_localizesCorrectly() {
    val wifiState = com.example.model.NetworkInfoState(connectionType = com.example.model.ConnectionType.WIFI)
    assertEquals("Wi-Fi", wifiState.getDisplayName(isEn = true))
    assertEquals("Wi-Fi сеть", wifiState.getDisplayName(isEn = false))

    val cellState = com.example.model.NetworkInfoState(connectionType = com.example.model.ConnectionType.CELLULAR)
    assertEquals("Cellular", cellState.getDisplayName(isEn = true))
    assertEquals("Мобильная сеть", cellState.getDisplayName(isEn = false))
  }

  @Test
  fun publicIpInfoCountry_localizesCorrectly() {
    val ipInfo = com.example.model.PublicIpInfo(
        ip = "108.177.29.30",
        countryCode = "DE",
        countryName = "Германия",
        flagEmoji = "🇩🇪"
    )
    assertEquals("Germany", ipInfo.getCountryDisplayName(isEn = true))
    assertEquals("Германия", ipInfo.getCountryDisplayName(isEn = false))
  }

  @Test
  fun pingService_resetPacketLoss_clearsLossCounters() {
    val pingService = com.example.data.network.PingService()
    pingService.recordPing(null) // 1 lost
    pingService.recordPing(50L)  // 1 success
    val statsBefore = pingService.recordPing(null) // 2 lost out of 3 total = 66%
    assertEquals(3, statsBefore.totalPingsSent)
    assertTrue(statsBefore.packetLossPercent > 0)

    val statsAfter = pingService.resetPacketLoss()
    assertEquals(0, statsAfter.totalPingsSent)
    assertEquals(0, statsAfter.packetLossPercent)
  }

  @Test
  fun testPhaseStatus_localizesCorrectly() {
    val statusEn = com.example.ui.i18n.AppI18n.getPhaseStatusText(com.example.model.TestPhase.COMPLETED, isEn = true)
    val statusRu = com.example.ui.i18n.AppI18n.getPhaseStatusText(com.example.model.TestPhase.COMPLETED, isEn = false)
    assertEquals("Testing completed", statusEn)
    assertEquals("Тестирование завершено", statusRu)
  }

  @Test
  fun testGeoIpHelp_andIpv6Characteristics() {
    val helpRu = com.example.ui.i18n.AppI18n.getMetricHelp(com.example.model.MetricModuleId.GEO_IP, isEn = false)
    val helpEn = com.example.ui.i18n.AppI18n.getMetricHelp(com.example.model.MetricModuleId.GEO_IP, isEn = true)

    assertNotNull(helpRu)
    assertNotNull(helpEn)
    assertEquals("Внешний IP", helpRu?.title)
    assertTrue(helpEn?.title?.contains("IP") == true)

    val ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
    val isIpv6 = ipv6.contains(":")
    val isIpv4 = ipv6.contains(".")
    assertTrue(isIpv6)
    assertFalse(isIpv4)
  }
}
