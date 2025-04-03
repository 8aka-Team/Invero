package cc.trixey.invero.common.message

import cc.trixey.invero.common.util.replaceNonEscaped
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.internal.parser.TokenParser
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.SECTION_CHAR
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.BukkitPlugin

/**
 * Message
 *
 * @author TheFloodDragon
 * @since 2025/3/8 14:33
 */
object Message {

    @JvmStatic
    val bukkitAudiences by lazy {
        BukkitAudiences.create(BukkitPlugin.getInstance())
    }

    @JvmStatic
    val gsonBuilder by lazy {
        if (MinecraftVersion.isLower(MinecraftVersion.V1_16)) {
            GsonComponentSerializer.colorDownsamplingGson()
        } else GsonComponentSerializer.gson()
    }

    @JvmStatic
    val legacyBuilder by lazy {
        LegacyComponentSerializer.builder().also {
            if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_16)) {
                it.hexColors()
                it.useUnusualXRepeatedCharacterHexFormat()
            }
        }.build()
    }

    @JvmStatic
    val miniBuilder by lazy {
        MiniMessage.miniMessage()
    }

    /**
     * 冒险API消息解析
     */
    @JvmStatic
    fun parseAdventure(source: String): Component {
        val parsed = legacyBuilder.deserialize(translateAmpersandColor(mark(source)))
            .let { miniBuilder.serialize(it) }
            .let { miniBuilder.deserialize(deMark(it)) }
        // https://github.com/KyoriPowered/adventure/issues/534
        return if (!parsed.hasDecoration(TextDecoration.ITALIC)) {
            parsed.decoration(TextDecoration.ITALIC, false);
        } else parsed
    }

    /**
     * 将 [Component] 转换成 Json字符串
     */
    @JvmStatic
    fun transformToJson(component: Component): String = gsonBuilder.serialize(component)

    /**
     * 将 Json字符串 转换成 [Component]
     */
    @JvmStatic
    fun transformFromJson(json: String): Component = gsonBuilder.deserialize(json)

    /**
     * 将 '&' 转换成 '§'
     */
    @JvmStatic
    fun translateAmpersandColor(target: String) = target.replace(AMPERSAND_CHAR, SECTION_CHAR)

    /**
     * 将 '§' 转换成 '&'
     */
    @JvmStatic
    fun translateLegacyColor(target: String) = target.replace(SECTION_CHAR, AMPERSAND_CHAR)

    @JvmStatic
    private fun mark(source: String) =
        source.replaceNonEscaped(TAG_START, MARKED_TAG_START).replaceNonEscaped(TAG_END, MARKED_TAG_END)

    @JvmStatic
    private fun deMark(source: String) =
        source.replace(MARKED_TAG_START, TAG_START).replace(MARKED_TAG_END, TAG_END)

    private const val MARKED_TAG_START = "MARKED_START"
    private const val MARKED_TAG_END = "MARKED_END"

    @Suppress("UnstableApiUsage")
    private const val TAG_START = TokenParser.TAG_START.toString()

    @Suppress("UnstableApiUsage")
    private const val TAG_END = TokenParser.TAG_END.toString()

}