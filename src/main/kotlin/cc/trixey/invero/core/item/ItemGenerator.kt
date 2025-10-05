package cc.trixey.invero.core.item

import cc.trixey.invero.common.message.writeDisplayName
import cc.trixey.invero.common.message.writeLore
import cc.trixey.invero.core.Context
import cc.trixey.invero.core.icon.IconElement
import cc.trixey.invero.core.util.flatRelease
import cc.trixey.invero.ui.bukkit.api.dsl.set
import cc.trixey.invero.ui.common.Scale
import com.google.common.collect.HashMultimap
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common5.cbool
import org.bukkit.NamespacedKey
import taboolib.common5.cint
import taboolib.common5.cshort
import taboolib.library.xseries.XEnchantment
import taboolib.module.nms.ItemTag
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir
import taboolib.platform.util.modifyMeta
import kotlin.jvm.optionals.getOrNull

/**
 * Invero
 * cc.trixey.invero.core.item.ItemGenerator
 *
 * @author Arasple
 * @since 2023/3/16 23:07
 */
fun Frame.renderFor(containerScale: Scale, iconElement: IconElement, defaultFrame: Frame, setSlot: Boolean = true) {
    val context = iconElement.context
    val previous = iconElement.itemStack

    if (setSlot && slot != null) {
        iconElement.set(slot!!.flatRelease(containerScale))
    }

    if (texture == null) {
        iconElement.itemStack = previous.generateProperties(this, context, defaultFrame)
    } else {
        texture!!.generateItem(context) {
            iconElement.itemStack = generateProperties(this@renderFor, context, defaultFrame)
        }
    }
}

private fun ItemStack.generateProperties(
    frame: Frame,
    context: Context,
    defaultFrame: Frame
): ItemStack {
    if (isAir) return this
    val isDefaultFrame = frame == defaultFrame

    fun <T> frameBy(block: Frame.() -> T?): T? {
        return if (!isDefaultFrame) frame.block() ?: defaultFrame.block()
        else frame.block()
    }

    return modifyMeta<ItemMeta> {
        // 显示名称
        frameBy { name }?.let { input ->
            writeDisplayName(context.parse(input).prefixColored)
        }
        // 显示描述
        frameBy { lore }?.let { input ->
            writeLore(context.parse(input).loreColored(frame.enhancedLore))
        }
        // [属性] 数量
        frameBy { amount }?.let {
            amount = (frame.staticAmount ?: context.parse(it.content).cint).coerceAtLeast(0)
        }
        // [属性] 损伤值
        frameBy { damage }?.let {
            @Suppress("DEPRECATION")
            durability = frame.staticDamage ?: context.parse(it.content).cshort
        }
        // [属性] 模型数据
        frameBy { customModelData }?.let {
            @Suppress("DEPRECATION")
            setCustomModelData(frame.staticCustomModelData ?: context.parse(it.content).cint)
        }
        // [属性] 物品模型 (1.21.2+)
        if (MinecraftVersion.versionId >= 12102) {
            frameBy { itemModel }?.let { raw ->
                val modelId = (frame.staticItemModel ?: context.parse(raw).trim())
                val key = modelId.ifBlank { null }?.let { NamespacedKey.fromString(it) }
                try {
                    this.itemModel = key
                } catch (_: Throwable) {
                    // 忽略低版本服务端或运行期不支持
                }
            }
        }
        // [属性] 是否发光
        frameBy { glow }?.let {
            if (frame.staticGlow == true || context.parse(it.content).cbool) {
                addItemFlags(ItemFlag.HIDE_ENCHANTS)
                if (this is EnchantmentStorageMeta) addStoredEnchant(Enchantment.LURE, 1, true)
                else addEnchant(Enchantment.LURE, 1, true)
            } else try {
                removeEnchantment(Enchantment.LURE)
                removeEnchant(Enchantment.LURE)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        // [属性] 不可破坏
        frameBy { unbreakable }?.let {
            if (frame.staticUnbreakable == true || context.parse(it.content).cbool) {
                isUnbreakable = true
            }
        }
        // [属性] 物品标签
        frameBy { flags }?.forEach {
            val flag = ItemFlag.valueOf(it.replace(' ', '_').uppercase())
            addItemFlags(flag)

            // 傻逼玩意儿
            if (flag == ItemFlag.HIDE_ATTRIBUTES && MinecraftVersion.versionId >= 12100) {
                attributeModifiers = HashMultimap.create<Attribute, AttributeModifier>()
            }
        }
        // [属性] 附魔
        frameBy { enchantments }?.forEach { (enchantment, level) ->
            val enchant = XEnchantment.of(enchantment).getOrNull()?.get()
            if (enchant != null) this.enchants[enchant] = level
        }
    }.also { itemStack ->
        // [属性] NBT
        frameBy { nbt }?.let {
            ItemTag().apply {
                putAll(getItemTag())
                if (frame.staticNBT != null) putAll(frame.staticNBT)
                else putAll(frame.buildNBT { s -> context.parse(s) })
                saveTo(itemStack)
            }
        }
    }
}

private val String.prefixColored: String
    get() = if (!startsWith("§") && isNotBlank()) "§7$this"
    else this

private fun List<String>.loreColored(enhancedProcess: Boolean?): List<String> {
    return if (enhancedProcess != true) {
        map { it.prefixColored }
    } else {
        val iterator = iterator()

        buildList {
            while (iterator.hasNext()) {
                val it = iterator.next()
                this += it.split("\\n").map { it.prefixColored }
            }
        }
    }
}