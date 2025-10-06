package cc.trixey.invero.core.serialize

/**
 * Invero
 * cc.trixey.invero.core.serialize.Presets
 *
 * @author Arasple
 * @since 2023/2/26 17:49
 */
internal val displayKeys = arrayOf(
    "material", "texture", "mat",
    "name",
    "lore", "lores",
    "amount", "count", "amt",
    "damage", "durability", "dur",
    "customModelData", "model",
    // 1.21.2+ 新增物品模型组件
    "itemModel", "item_model", "item-model",
    "color",
    // 1.20.5+ 新增隐藏物品信息框组件
    "hideTooltip", "hide_tooltip", "hide-tooltip",
    "glow", "shiny",
    "enchantments", "enchantment", "enchant",
    "flags", "flag",
    "unbreakable",
    "nbt",
    "enhancedLore",
    "slot", "slots", "pos", "position", "positions"
)

internal val textureKeysHead = arrayOf(
    "head", "skull"
)

internal val textureSourcedKeys = arrayOf(
    "zaphkiel", "zap",
    "oraxen",
    "itemsadder", "ia",
    "azureflow", "af",
    "itemtools", "it",
    "craftengine", "ce",
    "hmccosmetics", "hmc",
    "sxitem", "si",
    "mmoitems", "mi",
    "ecoitems", "eco",
    "magiccosmetics", "magic",
    "magicgem", "gem",
    "nexo",
    "slimefun", "sf",
    "headdatabase", "hdb",
    "base64", "json", "serialized",
    "kether"
)