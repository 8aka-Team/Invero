package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import com.willfp.ecoitems.items.EcoItems
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["ecoitems", "eco"])
class EcoItemsProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "EcoItems"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return EcoItems.getByID(identifier)?.itemStack
    }
}
