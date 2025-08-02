package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI
import org.bukkit.inventory.ItemStack

/**
 * @author postyizhan
 */
@DefItemProvider(["hmccosmetics", "hmc"])
class HMCCosmeticsItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "HMCCosmetics"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            HMCCosmeticsAPI.getCosmetic(identifier)?.item
        } catch (e: Exception) {
            null
        }
    }
}
