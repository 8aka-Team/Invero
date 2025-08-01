@file:OptIn(ExperimentalSerializationApi::class)

package cc.trixey.invero.core.menu

import cc.trixey.invero.common.util.prettyPrint
import cc.trixey.invero.core.Context
import cc.trixey.invero.core.Session
import cc.trixey.invero.core.menu.NodeRunnable.Type.*
import cc.trixey.invero.core.serialize.NodeTypeSerializer
import cc.trixey.invero.core.util.KetherHandler
import cc.trixey.invero.core.util.runJS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.entity.Player

/**
 * Invero
 * cc.trixey.invero.core.menu.NodeRunnable
 *
 * @author Arasple
 * @since 2023/2/8 11:34
 */
@Serializable
class NodeRunnable(
    @JsonNames("handler")
    val type: Type,
    @SerialName("value")
    @JsonNames("runnable", "run", "script")
    val _value: JsonPrimitive,
    @JsonNames("throwError")
    val throwable: Boolean? = null
) {

    @Transient
    val value = _value.content

    fun invoke(session: Session, variables: Map<String, Any>): String {
        val context = session.getVariable("@context") as? Context
        val viewer = session.viewer.get<Player>()

        return try {
            when (type) {
                CONST -> value
                TEXT -> session.parse(value)
                KETHER -> KetherHandler.invoke(value, viewer, session.getVariables(context?.variables) + variables)
                    .getNow("<TIMEOUT: KETHER>")
                JAVASCRIPT -> runJS(value, session, variables)
                    .getNow("<TIMEOUT: JAVASCRIPT>")
            }.toString()
        } catch (e: Throwable) {
            if (throwable != false) e.prettyPrint()
            "<ERROR: ${e.localizedMessage}>"
        }
    }

    @Serializable(with = NodeTypeSerializer::class)
    enum class Type {

        CONST,

        TEXT,

        KETHER,

        JAVASCRIPT

    }

}