package cc.trixey.invero.common.api

import cc.trixey.invero.core.menu.NodeRunnable

/**
 * Invero
 * cc.trixey.invero.common.api.InveroGlobalNodeManager
 *
 * 全局节点管理器接口
 * 负责管理可被所有菜单调用的全局node节点
 *
 * @author postyizhan
 * @since 2025/8/1 12:00
 */
interface InveroGlobalNodeManager {

    /**
     * 获取指定名称的全局节点
     *
     * @param name 节点名称
     * @return 节点实例，如果不存在则返回null
     */
    fun getNode(name: String): NodeRunnable?

    /**
     * 获取所有全局节点
     *
     * @return 全局节点映射表
     */
    fun getAllNodes(): Map<String, NodeRunnable>

    /**
     * 注册一个全局节点
     *
     * @param name 节点名称
     * @param node 节点实例
     */
    fun registerNode(name: String, node: NodeRunnable)

    /**
     * 注销一个全局节点
     *
     * @param name 节点名称
     * @return 是否成功注销
     */
    fun unregisterNode(name: String): Boolean

    /**
     * 检查指定名称的全局节点是否存在
     *
     * @param name 节点名称
     * @return 是否存在
     */
    fun hasNode(name: String): Boolean

    /**
     * 重新加载全局节点配置
     */
    fun reloadNodes()

    /**
     * 清空所有全局节点
     */
    fun clearNodes()

    /**
     * 获取全局节点数量
     *
     * @return 节点数量
     */
    fun getNodeCount(): Int

    /**
     * 获取所有全局节点的名称列表
     *
     * @return 节点名称列表
     */
    fun getNodeNames(): Set<String>

    /**
     * 将指定节点序列化为JSON字符串
     *
     * @param nodeName 节点名称
     * @return JSON字符串，如果节点不存在则返回null
     */
    fun serializeNodeToJson(nodeName: String): String?

}
