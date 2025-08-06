package cc.trixey.invero.core.compat.activators

import cc.trixey.invero.common.Invero
import cc.trixey.invero.common.Menu
import cc.trixey.invero.common.MenuActivator
import cc.trixey.invero.core.compat.DefActivator
import cc.trixey.invero.ui.bukkit.util.synced
import kotlinx.serialization.json.*
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.common.platform.command.component.CommandComponent
import taboolib.common.platform.function.unregisterCommand

/**
 * Invero
 * cc.trixey.invero.core.compat.activators.ActivatorCommand
 *
 * @author Arasple
 * @since 2023/2/25 15:03
 */
@DefActivator(["command", "commands", "cmd", "cmds"])
class ActivatorCommand(val command: JsonElement) : MenuActivator<ActivatorCommand>() {

    constructor() : this(JsonPrimitive(0))

    private val registeredCommands = mutableSetOf<String>()

    override fun setActivatorMenu(menu: Menu) {
        super.setActivatorMenu(menu)

        synced {
            when (command) {
                is JsonPrimitive -> registerCommandLabel(menu, command)
                is JsonObject -> registerCommandStructure(menu, command)
                is JsonArray -> {
                    if (command.firstOrNull() is JsonPrimitive) {
                        command.forEach { registerCommandLabel(menu, it as JsonPrimitive) }
                    } else {
                        command.forEach { registerCommandStructure(menu, it as JsonObject) }
                    }
                }
            }
        }
    }

    override fun unregister() {
        synced {
            registeredCommands.forEach { unregisterCommand(it) }
            registeredCommands.clear()
        }
        super.unregister()
    }

    private fun registerCommandStructure(menu: Menu, jsonObject: JsonObject) {
        Invero
            .API
            .getMenuManager()
            .getJsonSerializer<Json>()
            .decodeFromJsonElement<CommandStructure>(jsonObject)
            .apply {
                val perm = permission ?: ""
                command(
                    name,
                    aliases ?: emptyList(),
                    description ?: "",
                    usage ?: "",
                    perm,
                    permissionMessage ?: "",
                    permissionDefault = if (perm.isEmpty()) PermissionDefault.TRUE else PermissionDefault.OP
                ) {
                    // 无参数或没有必选参数，则添加默认执行为打开菜单
                    if (arguments.isNullOrEmpty() || arguments.all { it.optional }) {
                        execute<Player> { sender, _, _ -> menu.open(sender) }
                    }
                    // 标记当前层
                    var layer: CommandComponent = this
                    // 记录截至目前每层的参数
                    val impl = mutableSetOf<String>()
                    // 遍历参数实现
                    arguments?.forEach { argument ->
                        val id = argument.id.also { impl += it }
                        val type = argument.type ?: CommandArgument.Type.ANY
                        val default = argument.default
                        val restrict = argument.restrict
                        val suggest = argument.suggest ?: emptyList()

                        layer.dynamic(id, optional) {
                            execute<Player> { sender, ctx, _ ->
                                val variables = buildMap {
                                    impl.forEach { key ->
                                        val value = ctx.getOrNull(key) ?: default ?: error("No valid value")
                                        put(key, value)
                                    }
                                }
                                menu.open(sender, variables)
                            }
                            when (type) {
                                CommandArgument.Type.ANY -> suggestion<Player>(!restrict) { _, _ -> suggest }
                                CommandArgument.Type.DECIMAL -> restrictDouble()
                                CommandArgument.Type.INTEGER -> restrictInt()
                                CommandArgument.Type.BOOLEAN -> restrictBoolean()
                                CommandArgument.Type.PLAYER -> suggestPlayers(suggest)
                                CommandArgument.Type.WORLD -> suggestWorlds(suggest)
                            }
                        }.also { layer = it }
                    }
                }
                registeredCommands += name
                aliases?.map { it.lowercase() }?.let { registeredCommands += it }
            }
    }

    private fun registerCommandLabel(menu: Menu, jsonPrimitive: JsonPrimitive) {
        val id = menu.id ?: return
        val content = jsonPrimitive.contentOrNull?.lowercase() ?: return

        command(content, permissionDefault = PermissionDefault.TRUE) {
            execute<Player> { sender, _, _ ->
                Invero.API.getMenuManager().getMenu(id)?.open(sender)
            }
        }
        registeredCommands += content
    }

    override fun deserialize(element: JsonElement): ActivatorCommand {
        return ActivatorCommand(element)
    }

    override fun serialize(activator: ActivatorCommand) = activator.command

}
