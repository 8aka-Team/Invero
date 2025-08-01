package cc.trixey.invero.core.script

import cc.trixey.invero.common.Invero
import cc.trixey.invero.common.api.InveroSettings
import cc.trixey.invero.core.BaseMenu
import cc.trixey.invero.core.menu.NodeRunnable
import cc.trixey.invero.core.script.session
import taboolib.common.platform.function.submitAsync
import cc.trixey.invero.core.script.loader.InveroKetherParser
import taboolib.module.kether.combinationParser
import java.util.concurrent.CompletableFuture

/**
 * Invero
 * cc.trixey.invero.core.script.kether.ActionInvokeNode
 *
 * @author Arasple
 * @since 2023/2/2 14:14
 */
object ActionInvokeNode {

    @InveroKetherParser(["node"])
    fun node() = combinationParser {
        it.group(
            // 节点名称
            text(),
            // 参数
            command("with", then = action()).option().defaultsTo(null)
        ).apply(it) { nodeName, with ->
            future {
                val session = session()
                val menu = session?.menu as? BaseMenu ?: return@future CompletableFuture.completedFuture(false)
                val params = with
                    ?.let { w -> newFrame(w).run<Any>().getNow(null) }
                    .toParameter()
                    .let { strings -> mapOf("invokeArgs" to strings) }
                val node = findNode(menu, nodeName)
                    ?: return@future CompletableFuture.completedFuture(
                        "<NODE: $nodeName> 可用节点: ${getAvailableNodes(menu)}"
                    )
                val result = node.invoke(session, params)

                CompletableFuture.completedFuture(result)
            }
        }
    }

    @InveroKetherParser(["task"])
    fun task() = combinationParser {
        it.group(
            text(),
            command("with", then = action()).option().defaultsTo(null)
        ).apply(it) { name, with ->
            future {
                val session = session()
                val menu = session?.menu as? BaseMenu ?: return@future CompletableFuture.completedFuture(false)
                val params = with
                    ?.let { w -> newFrame(w).run<Any>().getNow(null) }
                    .toParameter()
                    .let { strings -> mapOf("invokeArgs" to strings) }
                val task = menu.tasks?.get(name)
                    ?: return@future CompletableFuture.completedFuture("<TASK: $name> ${menu.nodes?.keys}")

                CompletableFuture.completedFuture(submitAsync {
                    task.run(session, params)
                })
            }
        }
    }

    private fun Any?.toParameter(): List<String> = when (this) {
        null -> emptyList()
        is Collection<*> -> map { it.toString() }
        else -> listOf(toString())
    }

    /**
     * 根据配置的优先级查找节点
     * 支持菜单本地节点和全局节点的查找
     */
    private fun findNode(menu: BaseMenu, nodeName: String): NodeRunnable? {
        if (!InveroSettings.globalNodesEnabled) {
            // 如果全局节点功能未启用，只查找菜单本地节点
            return menu.nodes?.get(nodeName)
        }

        return when (InveroSettings.globalNodePriority) {
            "menu-first" -> {
                // 优先查找菜单本地节点，再查找全局节点
                menu.nodes?.get(nodeName) ?: Invero.API.getGlobalNodeManager().getNode(nodeName)
            }
            "global-first" -> {
                // 优先查找全局节点，再查找菜单本地节点
                Invero.API.getGlobalNodeManager().getNode(nodeName) ?: menu.nodes?.get(nodeName)
            }
            else -> {
                // 默认行为，向后兼容
                menu.nodes?.get(nodeName)
            }
        }
    }

    /**
     * 获取可用节点列表，用于错误提示
     */
    private fun getAvailableNodes(menu: BaseMenu): String {
        val menuNodes = menu.nodes?.keys ?: emptySet()
        val globalNodes = if (InveroSettings.globalNodesEnabled) {
            Invero.API.getGlobalNodeManager().getNodeNames()
        } else {
            emptySet()
        }

        val allNodes = (menuNodes + globalNodes).sorted()
        return if (allNodes.isEmpty()) {
            "无可用节点"
        } else {
            "菜单节点: ${menuNodes.sorted()}, 全局节点: ${globalNodes.sorted()}"
        }
    }

}