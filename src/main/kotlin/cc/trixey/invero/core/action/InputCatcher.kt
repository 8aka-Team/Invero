@file:OptIn(ExperimentalSerializationApi::class)

package cc.trixey.invero.core.action

import cc.trixey.invero.core.Context
import cc.trixey.invero.core.action.InputCatcher.Type.CHAT
import cc.trixey.invero.core.action.InputCatcher.Type.SIGN
import cc.trixey.invero.core.serialize.ListStringSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.bukkit.entity.Player
import taboolib.module.nms.inputSign
import taboolib.platform.util.cancelNextChat
import taboolib.platform.util.nextChat

/**
 * Invero
 * cc.trixey.invero.core.action.InputCatcher
 *
 * @author Arasple
 * @since 2023/1/31 21:27
 */
@Serializable
class InputCatcher(
    val catch: String,
    val content: JsonElement?,
    @JsonNames("signIndex", "index", "line")
    val signLine: Int?,
    @Serializable(ListStringSerializer::class)
    val cancel: List<String>?,
    val beforeInput: Action?,
    val afterInput: Action?,
    val onRepeat: Action?,
    val onCancel: Action?,
) {

    @Transient
    val type = if (signLine != null || content != null) SIGN else CHAT

    private val signContent by lazy {
        (if (type == SIGN) content?.jsonArray?.map { it.jsonPrimitive.content } else null)
            ?: emptyList()
    }

    fun run(player: Player, context: Context, repeat: Int = 0, onComplete: () -> Unit = {}) {
        if (repeat == 0) beforeInput?.run(context)

        player.cancelNextChat(execute = false)
        when (type) {
            CHAT -> player.nextChat {
                it.contentResponse(player, context, repeat, onComplete)
            }

            SIGN -> player.inputSign(context.parse(signContent).toTypedArray()) {
                val content = if (signLine == null || signLine < 0) {
                    it.joinToString("") { line -> line }
                } else {
                    it[signLine.coerceIn(0..it.lastIndex)]
                }
                content.contentResponse(player, context, repeat, onComplete)
            }
        }
    }

    private fun String.contentResponse(player: Player, context: Context, repeat: Int, onComplete: () -> Unit) {
        // cancelled
        if (shouldCancel()) {
            onCancel?.run(context)
            return
        }
        // set variables
        context.contextVariables[catch] = this
        // run handler
        val success = afterInput?.run(context)?.getNow(true) ?: true
        if (!success) {
            onRepeat?.run(context)
            run(player, context, repeat = repeat + 1, onComplete = onComplete)
        } else {
            onComplete()
        }
    }

    private fun String.shouldCancel(): Boolean {
        return cancel?.any { it.equals(this, true) } == true
    }

    enum class Type {
        CHAT,

        SIGN
    }

}
