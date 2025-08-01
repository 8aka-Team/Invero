package cc.trixey.invero.common.api

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration

/**
 * Invero
 * cc.trixey.invero.common.api.InveroSettings
 *
 * @author Arasple
 * @since 2023/1/19 11:51
 */
object InveroSettings {

    @Config
    lateinit var conf: Configuration
        private set

    /**
     * 工作路径
     */
    @ConfigNode("Workspaces.paths")
    var workspaces = listOf<String>()
        private set

    /**
     * 数据库类型
     */
    @ConfigNode("Database.type")
    var databaseType = "SQLITE"
        private set

    /**
     * SQL 配置
     */
    @ConfigNode("Database.sql")
    var sqlSection: ConfigurationSection? = null
        private set

    /**
     * 文件名过滤
     */
    @ConfigNode("Workspaces.filter")
    var fileFilter = "^(?![#!]).*\\.(?i)(conf|hocon|yaml|yml|json)\$"
        private set

    /**
     * 是否启用自动 Kether 变量
     */
    @ConfigNode("Kether.auto-placeholder-translate")
    var autoPlaceholderTranslate = true
        private set

    /**
     * 是否默认使用虚拟菜单
     */
    @ConfigNode("Menu.virtual-by-default")
    var virtualByDefault = false
        private set

    /**
     * 允许在 Vanilla Inventory 中使用 Raw Title
     */
    @Suppress("unused")
    @ConfigNode("Menu.enable-raw-title-in-vanilla-inventory")
    var enableRawTitleInVanillaInventory = false
        private set(value) {
            field = value
            // Enable raw title in vanilla inventory if configured
            if (value) taboolib.module.ui.enableRawTitleInVanillaInventory()
        }

    /**
     * 是否启用更新检查
     */
    @ConfigNode("update-checker.enabled")
    var updateCheckerEnabled = true
        private set

    /**
     * 更新检查间隔（天）
     */
    @ConfigNode("update-checker.check-interval-days")
    var updateCheckerInterval = 1
        private set

    /**
     * 是否启用全局节点功能
     */
    @ConfigNode("GlobalNodes.enabled")
    var globalNodesEnabled = true
        private set

    /**
     * 全局节点工作路径
     */
    @ConfigNode("GlobalNodes.paths")
    var globalNodesPaths = listOf<String>()
        private set

    /**
     * 全局节点文件名过滤
     */
    @ConfigNode("GlobalNodes.filter")
    var globalNodesFilter = "^(?![#!]).*\\.(?i)(conf|hocon|yaml|yml|toml|json)\$"
        private set

    /**
     * 全局节点查找优先级
     * menu-first: 优先查找菜单本地节点，再查找全局节点
     * global-first: 优先查找全局节点，再查找菜单本地节点
     */
    @ConfigNode("GlobalNodes.priority")
    var globalNodePriority = "menu-first"
        private set

}