package cc.trixey.invero.common

import cc.trixey.invero.common.api.InveroAPI
import cc.trixey.invero.common.api.InveroSettings
import cc.trixey.invero.core.util.UpdateChecker
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang
import taboolib.platform.util.bukkitPlugin

/**
 * Invero
 * cc.trixey.invero.common.Invero
 *
 * @author Arasple
 * @since 2023/2/1 17:13
 */
object Invero : Plugin() {

    val API: InveroAPI by lazy { PlatformFactory.getAPI() }
    
    // 更新检查器
    val updateChecker by lazy { UpdateChecker() }

    override fun onEnable() {
        """
        §3 ___
        §3|_ _|§b_ ____   §3_____ §b_ __ §3______
        §3 | |§b| '_ \\ \\ §3/ / §b_ \\ '__§3/ _ \\
        §3 | |§b| | | \\ §3V /  §b__/ |   §3| (_) |
        §3|___|§b_| |_|\\§3_/ \\§b___|_|  §3\\___/
        
        §8Invero §7v${bukkitPlugin.description.version} §8- §7MC 1.21.8
        §7By Arasple, Maintained by 8aka Team
        §7QQ群: §f489868834 §8| §7GitHub: §fhttps://github.com/8aka-Team/Invero
        """.lines().forEach { console().sendMessage(it) }
        
        // 检查更新
        checkUpdate()
    }
    
    /**
     * 检查更新
     */
    private fun checkUpdate() {
        if (InveroSettings.updateCheckerEnabled) {
            updateChecker.sendUpdateInfoToConsole()
        }
    }
}