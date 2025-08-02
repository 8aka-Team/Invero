package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.Context
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine
import net.momirealms.craftengine.core.util.Key
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 * @since 2025/8/2 17:30
 */
@DefItemProvider(["craftengine", "ce"])
class CraftEngineItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "CraftEngine"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            val player = (context as? Context)?.viewer?.get<Player>() ?: return null
            val (namespace, id) = identifier.split(":", limit = 2)
            val key = Key(namespace, id)
            val item = CraftEngineItems.byId(key) ?: return null
            val craftPlayer = BukkitCraftEngine.instance().adapt(player)
            item.buildItemStack(craftPlayer)
        } catch (e: Exception) {
            // 如果出现异常（比如 CraftEngine 未安装），返回 null，让系统使用默认纹理
            null
        }
    }
}
