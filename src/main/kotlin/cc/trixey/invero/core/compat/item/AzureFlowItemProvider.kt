package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.Context
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import io.rokuko.azureflow.api.AzureFlowAPI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Invero
 * cc.trixey.invero.core.compat.item.AzureFlowItemProvider
 *
 * @author postyizhan
 * @since 2025/8/4
 */
@DefItemProvider(["azureflow", "af"])
class AzureFlowItemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "AzureFlow"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return try {
            val player = (context as? Context)?.viewer?.get<Player>()
            val factory = AzureFlowAPI.getFactory(identifier)
            factory?.build()?.itemStack(player)
        } catch (e: Exception) {
            null
        }
    }
}
