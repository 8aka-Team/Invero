package cc.trixey.invero.core.item

import cc.trixey.invero.core.icon.Slot
import cc.trixey.invero.core.serialize.ListSlotSerializer
import cc.trixey.invero.core.serialize.ListStringSerializer
import cc.trixey.invero.core.util.containsAnyPlaceholder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import taboolib.common5.cshort
import taboolib.module.kether.inferType
import taboolib.module.nms.ItemTag
import taboolib.module.nms.ItemTagData

/**
 * Invero
 * cc.trixey.invero.core.item.WindowFrame
 *
 * @author Arasple
 * @since 2023/1/16 11:51
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
class Frame(
    @JsonNames("last")
    val delay: Long?,
    @Serializable
    @JsonNames("material", "mat")
    var texture: Texture?,
    var name: String?,
    @Serializable(with = ListStringSerializer::class)
    @JsonNames("lores")
    var lore: List<String>?,
    @Serializable @JsonNames("count", "amt")
    var amount: JsonPrimitive?,
    @JsonNames("durability", "dur")
    var damage: JsonPrimitive?,
    @JsonNames("model")
    var customModelData: JsonPrimitive?,
    @JsonNames("item_model", "item-model")
    var itemModel: String?,
    var color: String?,
    @JsonNames("shiny")
    var glow: JsonPrimitive?,
    @JsonNames("enchantment", "enchant")
    var enchantments: Map<String, Int>?,
    @Serializable(with = ListStringSerializer::class)
    @JsonNames("flag")
    var flags: List<String>?,
    @JsonNames("hide_tooltip", "hide-tooltip")
    var hideTooltip: JsonPrimitive?,
    var unbreakable: JsonPrimitive?,
    var nbt: Map<String, JsonPrimitive>?,
    @Serializable(with = ListSlotSerializer::class)
    @JsonNames("slots", "pos", "position", "positions")
    var slot: List<Slot>?,
    var enhancedLore: Boolean?
) {

    init {
        // yaml |- 多行写法
        if (lore != null && lore!!.size == 1) {
            lore = lore!!.single().split("\n")
        }
    }

    @Transient
    internal val staticAmount = amount?.intOrNull

    @Transient
    internal val staticDamage = damage?.intOrNull?.cshort

    @Transient
    internal val staticCustomModelData = customModelData?.intOrNull

    @Transient
    internal val staticItemModel =
        if (itemModel == null || itemModel!!.containsAnyPlaceholder) null else itemModel

    @Transient
    internal val staticGlow = glow?.booleanOrNull

    @Transient
    internal val staticHideTooltip = hideTooltip?.booleanOrNull

    @Transient
    internal val staticUnbreakable = unbreakable?.booleanOrNull

    @Transient
    internal val staticNBT =
        if (nbt == null || nbt!!.values.any { it.content.containsAnyPlaceholder }) null
        else {
            val tag = ItemTag()
            nbt!!.forEach { (key, value) -> tag[key] = ItemTagData.toNBT(value.content.inferType()) }
            tag
        }

    fun buildNBT(translator: (String) -> String): ItemTag {
        val tag = ItemTag()
        nbt!!.forEach { (key, value) -> tag[key] = ItemTagData.toNBT(translator(value.content).inferType()) }
        return tag
    }

//    fun inheirt(frame: Frame) = arrayOf(
//        "texture",
//        "name",
//        "lore",
//        "amount",
//        "damage",
//        "customModelData",
//        "color",
//        "glow",
//        "enchantments",
//        "flags",
//        "unbreakable",
//        "nbt",
//        "slot",
//        "enhancedLore"
//    ).forEach {
//        if (getProperty<Any?>(it) == null) {
//            val copy = frame.getProperty<Any>(it)
//            if (copy is Texture) setProperty(it, copy.clone())
//            else setProperty(it, copy)
//        }
//    }

    fun inheirt(frame: Frame) {
        if (texture == null) texture = frame.texture?.clone() as? Texture
        if (name == null) name = frame.name
        if (lore == null) lore = frame.lore
        if (amount == null) amount = frame.amount
        if (damage == null) damage = frame.damage
        if (customModelData == null) customModelData = frame.customModelData
        if (itemModel == null) itemModel = frame.itemModel
        if (color == null) color = frame.color
        if (glow == null) glow = frame.glow
        if (hideTooltip == null) hideTooltip = frame.hideTooltip
        if (enchantments == null) enchantments = frame.enchantments
        if (flags == null) flags = frame.flags
        if (unbreakable == null) unbreakable = frame.unbreakable
        if (nbt == null) nbt = frame.nbt
        if (slot == null) slot = frame.slot
        if (enhancedLore == null) enhancedLore = frame.enhancedLore
    }
}