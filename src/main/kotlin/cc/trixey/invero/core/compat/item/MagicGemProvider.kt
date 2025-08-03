package cc.trixey.invero.core.compat.item

import cc.trixey.invero.common.ItemSourceProvider
import cc.trixey.invero.core.compat.DefItemProvider
import cc.trixey.invero.core.compat.PluginHook
import org.bukkit.inventory.ItemStack
import pku.yim.magicgem.gem.GemManager

/**
 * @author postyizhan
 */
@DefItemProvider(["magicgem", "gem"])
class MagicGemProvider : ItemSourceProvider, PluginHook() {

    override val pluginName = "MagicGem"

    override fun getItem(identifier: String, context: Any?): ItemStack? {
        return GemManager.getGemByName(identifier)?.realGem
    }
}
