package cc.trixey.invero.core.api

import cc.trixey.invero.common.api.InveroGlobalNodeManager
import cc.trixey.invero.common.api.InveroSettings
import cc.trixey.invero.common.util.findInJar
import cc.trixey.invero.common.util.standardJson
import cc.trixey.invero.core.menu.NodeRunnable
import cc.trixey.invero.core.serialize.NodeSerializer
import cc.trixey.invero.core.serialize.hocon.PatchedLoader
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getJarFile
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import taboolib.module.lang.sendLang
import taboolib.platform.util.bukkitPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Invero
 * cc.trixey.invero.core.api.DefaultGlobalNodeManager
 *
 * 默认全局节点管理器实现
 *
 * @author postyizhan
 * @since 2025/8/1 12:00
 */
class DefaultGlobalNodeManager : InveroGlobalNodeManager {

    private val globalNodes: ConcurrentHashMap<String, NodeRunnable> = ConcurrentHashMap()
    private val filePattern by lazy { Pattern.compile(InveroSettings.globalNodesFilter) }



    override fun getNode(name: String): NodeRunnable? {
        return globalNodes[name]
    }

    override fun getAllNodes(): Map<String, NodeRunnable> {
        return globalNodes.toMap()
    }

    override fun registerNode(name: String, node: NodeRunnable) {
        globalNodes[name] = node
        info("注册全局节点: $name")
    }

    override fun unregisterNode(name: String): Boolean {
        val removed = globalNodes.remove(name)
        if (removed != null) {
            info("注销全局节点: $name")
            return true
        }
        return false
    }

    override fun hasNode(name: String): Boolean {
        return globalNodes.containsKey(name)
    }

    override fun reloadNodes() {
        val oldCount = globalNodes.size
        globalNodes.clear()
        reloadGlobalNodes()
        console().sendLang("global-nodes-reload-success", oldCount, globalNodes.size)
    }

    /**
     * 重载全局节点（显示详细信息）
     */
    private fun reloadGlobalNodes() {
        try {
            val workspaces = initWorkspaces()
            if (workspaces.isEmpty()) {
                warning("没有找到有效的全局节点工作空间")
                return
            }

            console().sendLang("global-nodes-workspace-inited", workspaces.size)

            for (workspace in workspaces) {
                reloadFromWorkspace(workspace)
            }

        } catch (e: Exception) {
            console().sendLang("global-nodes-reload-failed", e.message ?: "Unknown error")
        }
    }

    /**
     * 从工作空间重载节点（显示详细信息）
     */
    private fun reloadFromWorkspace(workspace: File) {
        workspace.listFiles()?.forEach { file ->
            if (file.isFile && filePattern.matcher(file.name).matches()) {
                reloadFromFile(file)
            }
        }
    }

    /**
     * 从单个文件重载节点（显示详细信息）
     */
    private fun reloadFromFile(file: File) {
        try {
            console().sendLang("global-nodes-file-loading", file.name)

            val config = PatchedLoader.loadFromFile(file, Type.JSON)
            val nodesSection = config.getConfigurationSection("nodes") ?: return

            for (nodeName in nodesSection.getKeys(false)) {
                try {
                    val nodeConfig = nodesSection.getConfigurationSection(nodeName) ?: continue
                    val nodeData = convertToJsonObject(nodeConfig)
                    val node = standardJson.decodeFromJsonElement<NodeRunnable>(NodeSerializer, nodeData)

                    globalNodes[nodeName] = node
                    console().sendLang("global-nodes-node-loaded", nodeName, node.type)
                } catch (e: Exception) {
                    console().sendLang("global-nodes-node-errored", nodeName, e.message ?: "Unknown error")
                }
            }

        } catch (e: Exception) {
            console().sendLang("global-nodes-file-errored", file.name)
        }
    }

    override fun clearNodes() {
        val count = globalNodes.size
        globalNodes.clear()
        info("清空了 $count 个全局节点")
    }

    override fun getNodeCount(): Int {
        return globalNodes.size
    }

    override fun getNodeNames(): Set<String> {
        return globalNodes.keys.toSet()
    }

    override fun serializeNodeToJson(nodeName: String): String? {
        val node = globalNodes[nodeName] ?: return null
        return try {
            standardJson.encodeToString(NodeSerializer, node)
        } catch (e: Exception) {
            console().sendLang("global-nodes-node-errored", nodeName, e.message ?: "Unknown error")
            null
        }
    }

    /**
     * 从配置文件加载全局节点
     */
    internal fun loadGlobalNodes() {
        try {
            val workspaces = initWorkspaces()
            if (workspaces.isEmpty()) {
                return
            }

            for (workspace in workspaces) {
                loadFromWorkspace(workspace)
            }

        } catch (e: Exception) {
            // 静默处理错误，只在重载时显示错误信息
        }
    }

    /**
     * 初始化工作空间
     */
    private fun initWorkspaces(): List<File> {
        val list = ArrayList<File>()

        for (path in InveroSettings.globalNodesPaths) {
            val file = File(path)
            // 如果不存在则释放默认文件
            if (!file.exists()) {
                releaseWorkspace(file)
            }
            if (file.isDirectory) list.add(file)
        }

        return list
    }

    /**
     * 从工作空间加载节点
     */
    private fun loadFromWorkspace(workspace: File) {
        workspace.listFiles()?.forEach { file ->
            if (file.isFile && filePattern.matcher(file.name).matches()) {
                loadFromFile(file)
            }
        }
    }

    /**
     * 从单个文件加载节点
     */
    private fun loadFromFile(file: File) {
        try {
            val config = PatchedLoader.loadFromFile(file, Type.JSON)
            val nodesSection = config.getConfigurationSection("nodes") ?: return

            for (nodeName in nodesSection.getKeys(false)) {
                try {
                    val nodeConfig = nodesSection.getConfigurationSection(nodeName) ?: continue
                    val nodeData = convertToJsonObject(nodeConfig)
                    val node = standardJson.decodeFromJsonElement<NodeRunnable>(NodeSerializer, nodeData)

                    globalNodes[nodeName] = node
                } catch (e: Exception) {
                    // 静默处理节点加载错误
                }
            }

        } catch (e: Exception) {
            // 静默处理文件加载错误
        }
    }

    /**
     * 释放默认工作空间
     */
    private fun releaseWorkspace(folder: File) {
        findInJar(getJarFile()) {
            !it.isDirectory && it.name.startsWith("default/global-nodes/")
        }.forEach {
            val targetFile = File(folder, it.first.name.substringAfter("default/global-nodes/"))
            targetFile.parentFile.mkdirs()
            targetFile.writeBytes(it.second.readBytes())
        }
    }

    /**
     * 将 TabooLib Configuration 转换为 JsonObject
     */
    private fun convertToJsonObject(config: taboolib.library.configuration.ConfigurationSection): JsonObject {
        val jsonBuilder = buildJsonObject {
            for (key in config.getKeys(false)) {
                val value = config.get(key)
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is List<*> -> {
                        val jsonArray = buildJsonArray {
                            value.forEach { item ->
                                when (item) {
                                    is String -> add(item)
                                    is Number -> add(item)
                                    is Boolean -> add(item)
                                    else -> add(item.toString())
                                }
                            }
                        }
                        put(key, jsonArray)
                    }
                    else -> if (value != null) put(key, value.toString())
                }
            }
        }
        return jsonBuilder
    }

    companion object {

        private val instance = DefaultGlobalNodeManager()

        @Awake(LifeCycle.INIT)
        fun init() {
            PlatformFactory.registerAPI<InveroGlobalNodeManager>(instance)
        }

        @Awake(LifeCycle.ACTIVE)
        fun onActive() {
            if (InveroSettings.globalNodesEnabled) {
                instance.loadGlobalNodes()
                console().sendLang("global-nodes-enabled", instance.globalNodes.size)
            }
        }
    }
}
