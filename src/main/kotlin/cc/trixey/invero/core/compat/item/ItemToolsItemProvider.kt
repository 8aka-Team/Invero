package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import com.github.cpjinan.plugin.itemtools.ItemTools
import org.bukkit.inventory.ItemStack

/**
 * Invero
 * cc.trixey.invero.core.compat.item.ItemToolsItemProvider
 *
 * @author postyizhan
 * @since 2025/8/6
 */
@DefItemProvider(["itemtools", "it"])
class ItemToolsItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "ItemTools"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            ItemTools.api().getManager().getItem(identifier)
        } catch (e: Exception) {
            null
        }
    }
}
