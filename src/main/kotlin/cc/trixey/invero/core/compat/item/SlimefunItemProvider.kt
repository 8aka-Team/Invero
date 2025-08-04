package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["slimefun", "sf"])
class SlimefunItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "Slimefun"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            SlimefunItem.getById(identifier)?.item?.clone()
        } catch (e: Exception) {
            null
        }
    }
}
