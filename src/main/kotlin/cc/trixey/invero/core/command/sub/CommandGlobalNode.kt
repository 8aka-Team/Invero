package cc.trixey.invero.core.command.sub

import cc.trixey.invero.common.Invero
import cc.trixey.invero.common.api.InveroSettings
import cc.trixey.invero.common.util.createContent
import cc.trixey.invero.common.util.paste

import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import cc.trixey.invero.core.command.createHelper
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestUncheck
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.sendLang
import java.util.concurrent.TimeUnit

/**
 * Invero
 * cc.trixey.invero.core.command.sub.CommandGlobalNode
 *
 * 全局节点管理命令
 *
 * @author postyizhan
 * @since 2025/8/1 12:00
 */
@CommandHeader(name = "gnode", aliases = ["globalnode", "gn"], permission = "invero.command.gnode", description = "Manage global nodes")
object CommandGlobalNode {

    @CommandBody
    val main = mainCommand { createHelper() }

    @CommandBody
    val list = subCommand {
        execute<CommandSender> { sender, _, _ ->
            if (!InveroSettings.globalNodesEnabled) {
                sender.sendLang("global-nodes-command-disabled")
                return@execute
            }

            val globalNodeManager = Invero.API.getGlobalNodeManager()
            val nodes = globalNodeManager.getAllNodes()

            if (nodes.isEmpty()) {
                sender.sendLang("global-nodes-command-list-empty")
                return@execute
            }

            sender.sendLang("global-nodes-command-list-header", nodes.size)
            nodes.forEach { (name, node) ->
                sender.sendLang("global-nodes-command-list-item", name, node.type)
            }
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, _, _ ->
            if (!InveroSettings.globalNodesEnabled) {
                sender.sendLang("global-nodes-command-disabled")
                return@execute
            }

            submitAsync {
                try {
                    val globalNodeManager = Invero.API.getGlobalNodeManager()
                    globalNodeManager.reloadNodes()
                } catch (e: Exception) {
                    sender.sendLang("global-nodes-reload-failed", e.message ?: "Unknown error")
                }
            }
        }
    }

    @CommandBody
    val info = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendLang("global-nodes-command-info-header")
            if (InveroSettings.globalNodesEnabled) {
                sender.sendLang("global-nodes-command-info-enabled")
            } else {
                sender.sendLang("global-nodes-command-info-disabled")
            }
            sender.sendLang("global-nodes-command-info-paths", InveroSettings.globalNodesPaths.joinToString(", "))
            sender.sendLang("global-nodes-command-info-priority", InveroSettings.globalNodePriority)

            if (InveroSettings.globalNodesEnabled) {
                val globalNodeManager = Invero.API.getGlobalNodeManager()
                sender.sendLang("global-nodes-command-info-count", globalNodeManager.getNodeCount())
            }
        }
    }

    @CommandBody
    val test = subCommand {
        execute<CommandSender> { sender, _, argument ->
            if (!InveroSettings.globalNodesEnabled) {
                sender.sendLang("global-nodes-command-disabled")
                return@execute
            }

            val nodeName = argument.removePrefix("test ")
            if (nodeName.isBlank()) {
                sender.sendLang("global-nodes-command-test-usage")
                return@execute
            }

            val globalNodeManager = Invero.API.getGlobalNodeManager()
            val node = globalNodeManager.getNode(nodeName)

            if (node == null) {
                sender.sendLang("global-nodes-command-test-not-found", nodeName)
                val availableNodes = globalNodeManager.getNodeNames()
                if (availableNodes.isNotEmpty()) {
                    sender.sendLang("global-nodes-command-test-available", availableNodes.joinToString(", "))
                }
                return@execute
            }

            sender.sendLang("global-nodes-command-test-found", nodeName)
            sender.sendLang("global-nodes-command-test-type", node.type)
            sender.sendLang("global-nodes-command-test-value", node.value)
            sender.sendLang("global-nodes-command-test-throw", node.throwable ?: "默认")
        }
    }

    /*
    dump [node_name]
     */
    @CommandBody
    val dump = subCommand {
        dynamic("node") {
            suggestUncheck {
                if (!InveroSettings.globalNodesEnabled) {
                    emptyList()
                } else {
                    Invero.API.getGlobalNodeManager().getNodeNames().toList()
                }
            }
            execute<CommandSender> { sender, ctx, _ ->
                if (!InveroSettings.globalNodesEnabled) {
                    sender.sendLang("global-nodes-command-disabled")
                    return@execute
                }

                val nodeName = ctx["node"] ?: return@execute
                val globalNodeManager = Invero.API.getGlobalNodeManager()
                val node = globalNodeManager.getNode(nodeName)

                if (node == null) {
                    sender.sendLang("global-nodes-command-test-not-found", nodeName)
                    return@execute
                }

                submitAsync {
                    val serialized = globalNodeManager.serializeNodeToJson(nodeName)
                    if (serialized == null) {
                        sender.sendLang("global-nodes-command-test-not-found", nodeName)
                        return@submitAsync
                    }

                    sender.sendLang("paste-init")

                    paste(
                        "Invero Global Node Serialization",
                        "global node serialized as json",
                        48,
                        TimeUnit.HOURS,
                        createContent("$nodeName.json", serialized, "JSON"),
                    ).apply {
                        when (status) {
                            cc.trixey.invero.common.util.PasteResult.Status.SUCCESS -> sender.sendLang("paste-success", anonymousLink)
                            cc.trixey.invero.common.util.PasteResult.Status.ERROR -> sender.sendLang("paste-failed")
                        }
                    }
                }
            }
        }
    }
}
