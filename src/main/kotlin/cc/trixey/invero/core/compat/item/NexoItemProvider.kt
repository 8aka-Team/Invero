package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import com.nexomc.nexo.api.NexoItems
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["nexo"])
class NexoItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "Nexo"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return NexoItems.itemFromId(identifier)?.build()
    }
}
