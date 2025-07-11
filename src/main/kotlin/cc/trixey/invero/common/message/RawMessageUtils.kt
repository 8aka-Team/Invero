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
import taboolib.module.chat.ComponentText
import taboolib.module.chat.colored
import taboolib.module.chat.component
import taboolib.module.nms.MinecraftVersion

// 缓存序列化器以避免重复创建
private val minecraftSerializer by lazy {
    try {
        MinecraftComponentSerializer.get()
    } catch (ex: UnsupportedOperationException) {
        null
    }
}

// 缓存 NMS 转换方法以避免重复反射
private val nmsConverters by lazy {
    try {
        val chatSerializerClass = Class.forName("net.minecraft.network.chat.Component\$Serializer")
        val fromJsonMethod = chatSerializerClass.getMethod("fromJson", String::class.java)
        Pair(chatSerializerClass, fromJsonMethod)
    } catch (e: Exception) {
        try {
            val craftChatMessageClass = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage")
            val fromJSONMethod = craftChatMessageClass.getMethod("fromJSON", String::class.java)
            Pair(craftChatMessageClass, fromJSONMethod)
        } catch (e2: Exception) {
            null
        }
    }
}

@Suppress("UnstableApiUsage")
fun Component.toMinecraft(): Any {
    // 优先使用缓存的序列化器
    minecraftSerializer?.let { serializer ->
        try {
            return serializer.serialize(this)
        } catch (ex: Exception) {
            // 如果序列化失败，继续到下面的逻辑
        }
    }

    // 如果没有可用的序列化器，使用 JSON 转换
    val json = Message.transformToJson(this)

    // 使用缓存的 NMS 转换器
    nmsConverters?.let { (_, method) ->
        try {
            return method.invoke(null, json) ?: json
        } catch (e: Exception) {
            // 转换失败，返回 JSON 字符串
        }
    }

    // 最终回退到 JSON 字符串
    return json
}

/**
 * 将 [Component] 写入物品的显示名称
 */
fun ItemMeta.writeDisplayName(raw: String): ItemMeta {
    if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)) {
        // Parse to Adventure Component
        val component: Component = Message.parseAdventure(raw)
        try {
            // Paper Method
            // void displayName(final net.kyori.adventure.text.@Nullable Component displayName)
            this.invokeMethod<Any>("displayName", component)
        } catch (ex: NoSuchMethodException) {
            // private IChatBaseComponent displayName;
            this.setProperty("displayName", component.toMinecraft())
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
            this.setProperty("lore", components.map { it.toMinecraft() })
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