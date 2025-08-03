package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.Context
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import github.saukiya.sxitem.SXItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["sxitem", "sx", "si"])
class SXItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "SX-Item"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            val player = (context as? Context)?.viewer?.get<Player>()
            player?.let {
                val result = SXItem.getItemManager().getItem(identifier, it)
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
