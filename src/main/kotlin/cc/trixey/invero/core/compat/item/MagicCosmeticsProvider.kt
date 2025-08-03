package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.Context
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import com.francobm.magicosmetics.api.MagicAPI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["magiccosmetics", "magic"])
class MagicCosmeticsProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "MagicCosmetics"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            // 支持两种格式：
            // 1. 直接物品ID：获取装饰品物品
            // 2. equipped:type 格式：获取玩家已装备的指定类型装饰品
            if (identifier.startsWith("equipped:")) {
                val type = identifier.substring(9)
                val player = (context as? Context)?.viewer?.get<Player>() ?: return null
                MagicAPI.getEquipped(player.name, type)
            } else {
                MagicAPI.getCosmeticItem(identifier)
            }
        } catch (e: Exception) {
            null
        }
    }
}
