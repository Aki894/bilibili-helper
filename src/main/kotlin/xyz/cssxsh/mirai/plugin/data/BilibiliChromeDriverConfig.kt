package xyz.cssxsh.mirai.plugin.data

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value
import kotlin.time.minutes

object BilibiliChromeDriverConfig : ReadOnlyPluginConfig("BilibiliChromeDriverConfig") {

    @ValueName("driver_path")
    val driverPath: String by value("")

    @ValueName("driver_url")
    val driverUrl: String by value("http://127.0.0.1:9515")

    @ValueName("chrome_path")
    val chromePath: String by value("")

    @ValueName("device_name")
    val deviceName: String by value("iPad")

    @ValueName("timeout_millis")
    val timeoutMillis: Long by value((1).minutes.toLongMilliseconds())
}