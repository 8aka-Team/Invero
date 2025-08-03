package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import net.Indyuce.mmoitems.MMOItems
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["mmoitems", "mi"])
class MMOItemsProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "MMOItems"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        val index = identifier.indexOf(':')
        if (index < 1 || index + 1 >= identifier.length) {
            return null
        }

        val type = identifier.substring(0, index)
        val id = identifier.substring(index + 1)

        return MMOItems.plugin.getItem(type, id)
    }
}
