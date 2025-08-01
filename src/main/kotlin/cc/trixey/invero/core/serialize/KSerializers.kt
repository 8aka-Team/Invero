@file:OptIn(ExperimentalSerializationApi::class)
package cc.trixey.invero.core.serialize

import cc.trixey.invero.common.util.standardJson
import cc.trixey.invero.core.Layout
import cc.trixey.invero.core.action.*
import cc.trixey.invero.core.animation.CycleMode
import cc.trixey.invero.core.menu.MenuTitle
import cc.trixey.invero.core.menu.NodeRunnable
import cc.trixey.invero.ui.common.Pos
import cc.trixey.invero.ui.common.Scale
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common5.cint
import taboolib.common5.cshort
import taboolib.module.nms.ItemTag
import taboolib.module.nms.getItemTag

/**
 * Invero
 * cc.trixey.invero.serialize.Serializers
 *
 * @author Arasple
 * @since 2023/1/18 0:12
 */

internal val listStringSerializer = ListSerializer(String.serializer())

object ItemStackJsonSerializer : KSerializer<ItemStack> {

    override val descriptor = PrimitiveSerialDescriptor("ItemStack.Json", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ItemStack {
        val itemObject = (decoder as JsonDecoder).decodeJsonElement().jsonObject

        itemObject.apply {
            val init = ItemStack(Material.valueOf(this["type"]?.jsonPrimitive?.content ?: "STONE"))

            this["amount"]?.jsonPrimitive?.intOrNull?.let { init.amount = it.cint }
            this["durability"]?.jsonPrimitive?.let { init.durability = it.cshort }
            this["meta"]?.jsonObject?.let {
                ItemTag.fromJson(it.toString()).saveTo(init, false)
            }

            return init
        }
    }

    override fun serialize(encoder: Encoder, value: ItemStack) {
        value.apply {
            (encoder as JsonEncoder).encodeJsonElement(
                buildJsonObject {
                    put("type", type.name)
                    if (amount > 1) put("amount", amount)
                    data!!.data.let { if (it > 0) put("data", it) }
                    val meta = standardJson.decodeFromString<JsonObject>(getItemTag().toLegacyJson())
                    if (meta.keys.isNotEmpty()) put("meta", meta)
                }
            )
        }
    }

}

object NodeTypeSerializer : KSerializer<NodeRunnable.Type> {

    override val descriptor = PrimitiveSerialDescriptor("NodeRunnable.Type", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NodeRunnable.Type =
        decoder.decodeString().tolerantEnum(NodeRunnable.Type.CONST)

    override fun serialize(encoder: Encoder, value: NodeRunnable.Type) = encoder.encodeString(value.name)

}

internal object ScaleSerializer : KSerializer<Scale> {

    private val intArraySerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("Scale", intArraySerializer.descriptor)

    override fun deserialize(decoder: Decoder) =
        decoder.decodeSerializableValue(intArraySerializer).let { Scale(it[0] to it[1]) }

    override fun serialize(encoder: Encoder, value: Scale) =
        encoder.encodeSerializableValue(intArraySerializer, intArrayOf(value.width, value.height))

}

internal object LayoutSerializer : KSerializer<Layout> {

    override val descriptor = SerialDescriptor("Layout", listStringSerializer.descriptor)

    override fun deserialize(decoder: Decoder) = try {
        decoder as JsonDecoder
        decoder
            .decodeSerializableValue(listStringSerializer)
            .let { Layout(it) }
    } catch (e: Throwable) {
        Layout(decoder.decodeString().split("\n"))
    }

    override fun serialize(encoder: Encoder, value: Layout) =
        encoder.encodeSerializableValue(listStringSerializer, value.raw)

}

internal object PosSerializer : KSerializer<Pos> {

    private val intArraySerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("Pos", intArraySerializer.descriptor)

    override fun deserialize(decoder: Decoder) =
        decoder.decodeSerializableValue(intArraySerializer).let { Pos(it[0], it[1]) }

    override fun serialize(encoder: Encoder, value: Pos) =
        encoder.encodeSerializableValue(intArraySerializer, value.value.let { intArrayOf(it.first, it.second) })

}

internal object ComparatorSerializer : KSerializer<Comparator> {

    override val descriptor = PrimitiveSerialDescriptor("Comparator", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Comparator {
        return Comparator.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Comparator) {
        return encoder.encodeString(value.toString())
    }

}

internal object NetesedActionSerializer : KSerializer<NetesedAction> {

    private val listSerializer = ListSerializer(Action.serializer())
    override val descriptor = buildClassSerialDescriptor("NetesedAction") { element<List<Action>>("actions") }

    override fun serialize(encoder: Encoder, value: NetesedAction) {
        encoder.encodeSerializableValue(listSerializer, value.actions)
    }


    override fun deserialize(decoder: Decoder): NetesedAction {
        return NetesedAction(decoder.decodeSerializableValue(listSerializer))
    }

}

internal object ActionKetherSerializer : KSerializer<ActionKether> {

    override val descriptor = buildClassSerialDescriptor("ActionKether") { element<String>("script") }

    override fun deserialize(decoder: Decoder): ActionKether {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return when (element) {
            is JsonArray -> element.mapNotNull { it.jsonPrimitive.contentOrNull }.joinToString("\n") { it }
            is JsonPrimitive -> element.content
            is JsonObject -> {
                val first = element.values.firstOrNull()
                if (first is JsonPrimitive) {
                    first.content
                } else {
                    error("Invalid Kether Script: $element")
                }
            }
        }.let { ActionKether(it) }
    }

    override fun serialize(encoder: Encoder, value: ActionKether) {
        encoder.encodeString(value.script)
    }

}

internal object ScriptKetherSerializer : KSerializer<ScriptKether> {

    override val descriptor = PrimitiveSerialDescriptor("ScriptKether", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ScriptKether {
        return ScriptKether((decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content)
    }

    override fun serialize(encoder: Encoder, value: ScriptKether) = encoder.encodeString(value.script)

}

internal object MenuTitleSerializer : JsonTransformingSerializer<MenuTitle>(MenuTitle.serializer()) {

    override fun transformDeserialize(element: JsonElement): JsonObject {
        return when (element) {
            is JsonPrimitive -> buildJsonObject {
                put(
                    "frames",
                    buildJsonArray { add(buildJsonObject { put("value", element.content) }) }
                )
            }

            is JsonObject ->
                if ("values" in element.keys) {
                    buildJsonObject {
                        element.forEach { (key, value) -> put(key, value) }
                        element["values"]!!
                            .jsonArray
                            .map { buildJsonObject { put("value", it.jsonPrimitive.content) } }
                            .let { put("frames", JsonArray(it)) }
                    }
                } else {
                    element
                }

            else -> error("MenuTitleSerializer does not support $element")
        }
    }

}

object CycleModeSerializer : KSerializer<CycleMode> {

    override val descriptor = PrimitiveSerialDescriptor("CycleMode", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CycleMode = decoder.decodeString().tolerantEnum(CycleMode.LOOP)

    override fun serialize(encoder: Encoder, value: CycleMode) = encoder.encodeString(value.name)

}

internal object InventoryTypeSerializer : KSerializer<InventoryType> {

    override val descriptor = PrimitiveSerialDescriptor("InventoryType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): InventoryType = decoder.decodeString().tolerantEnum(InventoryType.CHEST)

    override fun serialize(encoder: Encoder, value: InventoryType) = encoder.encodeString(value.name)

}


private inline fun <reified T : Enum<T>> String.tolerantEnum(default: T) = uppercase()
    .replace(' ', '_')
    .replace('-', '_')
    .let { input -> enumValues<T>().find { it.name == input } ?: default }