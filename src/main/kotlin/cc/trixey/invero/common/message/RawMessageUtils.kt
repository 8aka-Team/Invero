package cc.trixey.invero.common.message

import net.kyori.adventure.platform.bukkit.MinecraftComponentSerializer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.meta.ItemMeta
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.nms.MinecraftVersion

@Suppress("UnstableApiUsage")
fun Component.toMinecraft(): Any = MinecraftComponentSerializer.get().serialize(this)

/**
 * 将 [Component] 写入物品的显示名称
 */
fun ItemMeta.writeDisplayName(raw: String): ItemMeta {
    if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)) {
        // Parse to Adventure Component
        val component: Component = Message.parseAdventure(raw)
        try {
            // Paper Method
            this.invokeMethod<Any>("displayName", component)
        } catch (ex: NoSuchMethodException) {
            // Ensure compatibility with the expected type of "displayName"
            val minecraftComponent = component.toMinecraft()
            if (minecraftComponent is net.minecraft.network.chat.IChatBaseComponent) {
                this.setProperty("displayName", minecraftComponent)
            } else {
                throw IllegalArgumentException("Incompatible type for displayName: ${minecraftComponent::class.java}")
            }
        }
    } else this.setDisplayName(raw) // 低版本就不处理了
    return this
}

/**
 * 将 [Component] 写入物品的描述
 */
fun ItemMeta.writeLore(raw: List<String>): ItemMeta {
    if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)) {
        // Parse to Adventure Components
        val components: List<Component> = raw.map { Message.parseAdventure(it) }
        try {
            // Paper Method
            // void lore(final @Nullable List<? extends net.kyori.adventure.text.Component> lore);
            this.invokeMethod<Any>("lore", components)
        } catch (ex: NoSuchMethodException) {
            // private List<IChatBaseComponent> lore;
            // 使用toMinecraft()方法返回的对象列表，不进行类型转换
            val minecraftComponents = components.map { it.toMinecraft() }
            this.setProperty("lore", minecraftComponents)
        }
    } else this.lore = raw // 低版本就不处理了
    return this
}

internal fun createInventoryPaper(owner: InventoryHolder, type: InventoryType, title: String): Inventory {
    return createInventoryPaperIfSupported(owner, type, title) ?: Bukkit.createInventory(owner, type, title)
}

internal fun createInventoryPaper(owner: InventoryHolder, size: Int, title: String): Inventory {
    return createInventoryPaperIfSupported(owner, size, title) ?: Bukkit.createInventory(owner, size, title)
}

/**
 * 尝试一个 [Inventory]，支持 1.17+ 的 Adventure Component, 仅 Paper 可用
 */
private fun createInventoryPaperIfSupported(owner: InventoryHolder, typeOrSize: Any, title: String): Inventory? {
    if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)) {
        // Parse to Adventure Component
        val parsedTitle: Component = Message.parseAdventure(title)
        // Only Supported for Paper
        try {
            // Paper Method
            // public static Inventory createInventory(@Nullable InventoryHolder owner, @NotNull InventoryType type, net.kyori.adventure.text.@NotNull Component title)
            return Bukkit.getServer().invokeMethod<Inventory>("createInventory", owner, typeOrSize, parsedTitle)!!
        } catch (_: NoSuchMethodException) {
        }
    }
    return null
}
