package cc.trixey.invero.common.message

import cc.trixey.invero.core.util.KetherHandler
import cc.trixey.invero.core.util.replaceBitmapBlank
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.chat.HexColor
import taboolib.module.chat.component
import taboolib.module.chat.impl.AdventureComponent
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.compat.replacePlaceholder

/**
 * Invero
 * cc.trixey.invero.common.util.TextDecoration
 *
 * @author Arasple
 * @since 2023/2/22 13:06
 */

/**
 * 常规文本格式替换
 *
 * 文本源中的 § 保留为[[COLOR_CHAR]]
 * 替换 kether inline 变量
 * 替换 placeholderAPI 变量
 * 翻译通用颜色格式
 * 还原 [[COLOR_CHAR]] 为 §
 */
fun String.translateFormattedMessage(
    player: Player,
    variables: Map<String, Any?> = emptyMap(),
    skipComp: Boolean = false
) =
    KetherHandler.parseInline(replace("§", COLOR_CHAR), player, variables)
        .replacePlaceholder(player)
        .replace("§", COLOR_CHAR)
        .colored()
        .replace(COLOR_CHAR, "§")
        .replaceBitmapBlank()
        .let {
            if (skipComp) it else it.component().build().toLegacyText()
        }

/**
 * 文本转 [AdventureComponent]
 * @see Message.parseAdventure
 */
fun String.componentAdventure(): AdventureComponent = AdventureComponent(Message.parseAdventure(this))

private const val COLOR_CHAR = "[[COLOR_CHAR]]"

/**
 * （默认）发送格式化消息
 * 依次翻译
 * - Kether Inline
 * - Placeholder API
 * - Adventure MiniMessage
 */
fun String.sendFormattedAdventureComponent(player: Player, variables: Map<String, Any> = emptyMap()) {
    val processed = KetherHandler.parseInline(this, player, variables)
        .replacePlaceholder(player)

    val component = Message.parseAdventure(processed)

    Message.bukkitAudiences.player(player).sendMessage(component)
}

/**
 * 发送 TabooLib ComponentText
 * 依次翻译
 * - Kether Inline
 * - Placeholder API
 * - TabooLib Component (with colored)
 */
fun String.sendFormattedTabooComponent(player: Player, variables: Map<String, Any> = emptyMap()) =
    KetherHandler.parseInline(this, player, variables)
        .replacePlaceholder(player)
        .colored()
        .component()
        .build()
        .sendTo(adaptPlayer(player))

fun String.colored() =
    HexColor
        .translate(this)
        .let {
            if (patchDragonCore) {
                it.replace("&#", "§#")
            } else {
                it
            }
        }

private val patchDragonCore: Boolean = MinecraftVersion.versionId in 11903 downTo 11300