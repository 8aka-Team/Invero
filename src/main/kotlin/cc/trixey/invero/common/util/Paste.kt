package  cc.trixey.invero.common.util

import kotlinx.serialization.json.*
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Invero
 * cc.trixey.invero.common.util.Paste
 *
 * @author Arasple
 * @since 2023/2/13 20:45
 */

// 剪贴板配置
object PasteConfig {
    @Config("config.yml")
    private lateinit var config: Configuration
    private var serviceType = "pastegg"
    private val serviceConfigs = ConcurrentHashMap<String, Map<String, String>>()

    fun loadConfig() {
        try {
            // 检查配置是否存在paste部分，如果不存在则添加默认配置
            if (!config.contains("Paste")) {
                config["Paste.service-type"] = "pastegg"
                config["Paste.services.pastegg.base-url"] = "https://api.paste.gg/v1"
                config["Paste.services.lucko.base-url"] = "https://api.pastes.dev"
                config["Paste.services.pastebin.base-url"] = "https://pastebin.com/api/api_post.php"
                config["Paste.services.pastebin.dev-key"] = "PD5qZ5fXHr-rG4Xpl-nM-BDXmkUod_C8"
                config.saveToFile()
            }

            serviceType = config.getString("Paste.service-type", "pastegg") ?: "pastegg"

            val servicesSection = config.getConfigurationSection("Paste.services")
            if (servicesSection != null) {
                for (key in servicesSection.getKeys(false)) {
                    val serviceSection = servicesSection.getConfigurationSection(key)
                    if (serviceSection != null) {
                        val configMap = mutableMapOf<String, String>()
                        for (configKey in serviceSection.getKeys(false)) {
                            serviceSection.getString(configKey)?.let {
                                configMap[configKey] = it
                            }
                        }
                        serviceConfigs[key] = configMap
                    }
                }
            }
        } catch (e: Exception) {
            warning("无法加载剪贴板配置: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getServiceType(): String = serviceType

    fun getServiceConfig(service: String): Map<String, String> = serviceConfigs[service] ?: emptyMap()
}

// 剪贴板服务接口
interface PasteService {
    fun paste(
        name: String,
        description: String,
        content: List<PasteContent>,
        expiresIn: Long = -1,
        timeUnit: TimeUnit = TimeUnit.MINUTES
    ): PasteResult
}

// 工厂类创建具体的剪贴板服务实例
object PasteServiceFactory {
    fun createService(): PasteService {
        return when (PasteConfig.getServiceType().lowercase()) {
            "lucko" -> LuckoPasteService()
            "pastebin" -> PastebinService()
            else -> PasteGGService()
        }
    }
}

// PasteGG 实现
class PasteGGService : PasteService {
    private val isoInstant = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private val config = PasteConfig.getServiceConfig("pastegg")
    private val baseUrl = config["base-url"] ?: "https://api.paste.gg/v1"

    override fun paste(
        name: String,
        description: String,
        content: List<PasteContent>,
        expiresIn: Long,
        timeUnit: TimeUnit
    ): PasteResult {
        val jsonInput = buildJsonObject {
            put("name", name)
            put("description", description)
            put("visibility", "unlisted")
            if (expiresIn > 0) {
                val date = Date((System.currentTimeMillis() + timeUnit.toMillis(expiresIn)))
                put("expires", isoInstant.format(date))
            }
            put("files", buildJsonArray {
                content.forEach {
                    add(
                        buildJsonObject {
                            put("name", it.name)
                            put("content", buildJsonObject {
                                put("format", "text")
                                put("highlight_language", it.highlightLanguage)
                                put("value", it.value)
                            })
                        }
                    )
                }
            })
        }.let { Json.encodeToString(it) }

        val url = URL("$baseUrl/pastes")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.write(jsonInput.toByteArray(StandardCharsets.UTF_8))
        val response = conn.inputStream.reader().readLines().joinToString("\n")

        Json.decodeFromString<JsonObject>(response).apply {
            val status = PasteResult.Status.valueOf(this["status"]!!.jsonPrimitive.content.uppercase())
            val result = this["result"]?.jsonObject

            return PasteResult(
                status,
                result,
                "https://paste.gg/p/anonymous/${result?.get("id")?.jsonPrimitive?.content}"
            )
        }
    }
}

// Lucko Paste 实现
class LuckoPasteService : PasteService {
    private val config = PasteConfig.getServiceConfig("lucko")
    private val baseUrl = config["base-url"] ?: "https://api.pastes.dev"

    override fun paste(
        name: String,
        description: String,
        content: List<PasteContent>,
        expiresIn: Long,
        timeUnit: TimeUnit
    ): PasteResult {
        if (content.isEmpty()) {
            return PasteResult(PasteResult.Status.ERROR, null, "No content provided")
        }

        // Lucko paste API 比较简单，只需要发送内容即可
        val firstContent = content.first()
        val contentToSend = firstContent.value

        val url = URL("$baseUrl/post")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true

        // 设置语言标记
        if (firstContent.highlightLanguage != null) {
            conn.setRequestProperty("Content-Type", "text/${firstContent.highlightLanguage}")
        } else {
            conn.setRequestProperty("Content-Type", "text/plain")
        }

        conn.setRequestProperty("User-Agent", "Invero/1.0")
        conn.outputStream.write(contentToSend.toByteArray(StandardCharsets.UTF_8))

        // 检查响应码
        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            return PasteResult(PasteResult.Status.ERROR, null, "HTTP Error: $responseCode")
        }

        // 从 Location 头获取文档 ID
        val location = conn.getHeaderField("Location")
        val key = location?.substringAfterLast("/") ?: run {
            // 尝试从响应正文获取 key
            val response = conn.inputStream.reader().readLines().joinToString("\n")
            try {
                Json.decodeFromString<JsonObject>(response)["key"]?.jsonPrimitive?.content
            } catch (e: Exception) {
                null
            }
        }

        if (key == null) {
            return PasteResult(PasteResult.Status.ERROR, null, "Failed to get paste key")
        }

        return PasteResult(
            PasteResult.Status.SUCCESS,
            buildJsonObject { put("id", key) }.jsonObject,
            "https://pastes.dev/$key"
        )
    }
}

// Pastebin 实现
class PastebinService : PasteService {
    private val config = PasteConfig.getServiceConfig("pastebin")
    private val baseUrl = config["base-url"] ?: "https://pastebin.com/api/api_post.php"
    private val devKey = config["dev-key"] ?: ""

    override fun paste(
        name: String,
        description: String,
        content: List<PasteContent>,
        expiresIn: Long,
        timeUnit: TimeUnit
    ): PasteResult {
        if (content.isEmpty()) {
            return PasteResult(PasteResult.Status.ERROR, null, "No content provided")
        }

        // 准备参数
        val firstContent = content.first()
        val expireDate = when {
            expiresIn <= 0 -> "N" // 永不过期
            timeUnit.toMinutes(expiresIn) <= 10 -> "10M" // 10分钟
            timeUnit.toMinutes(expiresIn) <= 60 -> "1H" // 1小时
            timeUnit.toHours(expiresIn) <= 24 -> "1D" // 1天
            timeUnit.toDays(expiresIn) <= 7 -> "1W" // 1周
            timeUnit.toDays(expiresIn) <= 14 -> "2W" // 2周
            timeUnit.toDays(expiresIn) <= 30 -> "1M" // 1月
            else -> "1M" // 默认1月
        }

        // 转换语言格式
        val format = when (firstContent.highlightLanguage) {
            "bash" -> "bash"
            "c" -> "c"
            "cpp" -> "cpp"
            "csharp", "cs" -> "csharp"
            "css" -> "css"
            "html" -> "html5"
            "java" -> "java"
            "javascript", "js" -> "javascript"
            "json" -> "json"
            "lua" -> "lua"
            "markdown", "md" -> "markdown"
            "php" -> "php"
            "python", "py" -> "python"
            "ruby" -> "ruby"
            "sql" -> "sql"
            "xml" -> "xml"
            "yaml", "yml" -> "yaml"
            else -> "text"
        }

        // 构建请求参数
        val params = mapOf(
            "api_option" to "paste",
            "api_dev_key" to devKey,
            "api_paste_name" to name,
            "api_paste_code" to firstContent.value,
            "api_paste_format" to format,
            "api_paste_private" to "1", // Unlisted
            "api_paste_expire_date" to expireDate
        )

        // 发送请求
        val url = URL(baseUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val data = params.entries.joinToString("&") { (key, value) ->
            "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }

        conn.outputStream.write(data.toByteArray(StandardCharsets.UTF_8))

        // 检查响应
        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return PasteResult(PasteResult.Status.ERROR, null, "HTTP Error: $responseCode")
        }

        val response = conn.inputStream.reader().readText().trim()

        // 检查是否获取了URL
        return if (response.startsWith("https://pastebin.com/")) {
            val pasteId = response.substringAfterLast("/")
            PasteResult(
                PasteResult.Status.SUCCESS,
                buildJsonObject { put("id", pasteId) }.jsonObject,
                response
            )
        } else {
            PasteResult(PasteResult.Status.ERROR, null, "API Error: $response")
        }
    }
}

fun createContent(
    name: String,
    value: String,
    highlightLanguage: String? = null,
    block: PasteContent.() -> Unit = {}
): PasteContent {
    return PasteContent(name, value, highlightLanguage).also(block)
}

fun paste(
    name: String,
    description: String,
    last: Long = -1,
    unit: TimeUnit = TimeUnit.MINUTES,
    vararg content: PasteContent
): PasteResult {
    return PasteServiceFactory.createService().paste(name, description, content.toList(), last, unit)
}

class PasteResult(val status: Status, val result: JsonObject?, val url: String) {

    enum class Status {
        SUCCESS,
        ERROR
    }

    // 保持向后兼容
    val anonymousLink: String
        get() = url
}

data class PasteContent internal constructor(
    val name: String,
    val value: String,
    val highlightLanguage: String? = null
) {
    fun paste(description: String, last: Long = -1, unit: TimeUnit = TimeUnit.MINUTES) =
        paste(name, description, last, unit, this)
}