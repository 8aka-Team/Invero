package cc.trixey.invero.common.message

import org.bukkit.inventory.meta.ItemMeta
import taboolib.module.chat.impl.AdventureComponent
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.setDisplayNameComponent
import taboolib.module.nms.setLoreComponents

/**
 * RawMessageUtils
 *
 * @author TheFloodDragon
 * @since 2025/9/13 23:15
 */

/**
 * 文本转 [AdventureComponent]
 * @see Message.parseAdventure
 */
fun String.componentAdventure(): AdventureComponent = AdventureComponent(Message.parseAdventure(this))

/**
 * 向 [ItemMeta] 中写入 [lore] (提供跨版本支持)
 */
fun ItemMeta.writeLore(lore: List<String>) =
    if (MinecraftVersion.isUniversal) {
        setLoreComponents(lore.map { it.componentAdventure() })
    } else {
        this.lore = lore
    }

/**
 * 向 [ItemMeta] 中写入 [name] (提供跨版本支持)
 */
fun ItemMeta.writeDisplayName(name: String) =
    if (MinecraftVersion.isUniversal) {
        setDisplayNameComponent(name.componentAdventure())
    } else {
        setDisplayName(name)
    }

/**
 * 解析成 Json (提供跨版本支持)
 */
fun String.toRawOrNot() =
    if (MinecraftVersion.isUniversal) {
        componentAdventure().toRawMessage()
    } else {
        this
    }
