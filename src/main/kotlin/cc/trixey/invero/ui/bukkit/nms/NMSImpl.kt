package cc.trixey.invero.ui.bukkit.nms

import cc.trixey.invero.common.message.Message
import cc.trixey.invero.ui.common.ContainerType
import net.minecraft.server.v1_16_R3.*
import net.minecraft.world.inventory.Containers
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.library.reflex.Reflex.Companion.unsafeInstance
import taboolib.module.nms.MinecraftVersion.isUniversal
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.sendBundlePacketBlocking
import taboolib.module.nms.sendPacketBlocking

/**
 * Invero
 * cc.trixey.invero.ui.bukkit.nms.NMSImpl
 *
 * @author Arasple
 * @since 2022/10/20
 */

class NMSImpl : NMS {

    private val itemAir = null.asNMSCopy()

    override fun sendWindowOpen(player: Player, containerId: Int, type: ContainerType, rawTitle: String) {
        // 先将标题解析为 Adventure 组件，再序列化为 JSON
        // 交给 NMS 的 fromJSON 渲染，避免把 JSON 当作普通文本显示
        val title = runCatching {
            val component = Message.parseAdventure(rawTitle)
            val json = Message.transformToJson(component)
            CraftChatMessage.fromJSON(json)
        }.getOrElse {
            // 回退到传统解析，确保兼容极端输入
            CraftChatMessage.fromStringOrNull(rawTitle)
        }

        val instance = PacketPlayOutOpenWindow::class.java.unsafeInstance()

        when {
            isUniversal -> {
                player.postPacket(
                    instance,
                    "containerId" to containerId,
                    if (versionId < 11900) "type" to type.serialId
                    else "type" to Containers::class.java.getProperty<Containers<*>>(type.vanillaId, true),
                    "title" to title
                )
            }

            versionId >= 11400 -> {
                player.postPacket(
                    instance,
                    "a" to containerId,
                    "b" to type.serialId,
                    "c" to title
                )
            }

            else -> {
                player.postPacket(
                    instance,
                    "a" to containerId,
                    "b" to type.bukkitId,
                    "c" to title,
                    "d" to type.containerSize
                )
            }
        }
    }

    override fun sendWindowClose(player: Player, containerId: Int) {
        player.postPacket(PacketPlayOutCloseWindow(containerId))
    }

    private val fieldContainerID = if (versionId >= 11700) "containerId" else "a"
    private val fieldItems = if (versionId >= 11700) "items" else "b"

    override fun sendWindowItems(player: Player, containerId: Int, itemStacks: List<ItemStack?>) {
        val instance = PacketPlayOutWindowItems::class.java.unsafeInstance()
        val items = itemStacks.asNMSCopy()

        when {
            versionId >= 11701 -> {
                player.postPacket(
                    instance,
                    "containerId" to containerId,
                    "items" to items,
                    "carriedItem" to itemAir,
                    "stateId" to 1,
                )
            }

            else -> {
                val valueItems: Any = if (versionId >= 11000) items else items.toTypedArray()

                player.postPacket(
                    instance,
                    fieldContainerID to containerId,
                    fieldItems to valueItems
                )
            }
        }
    }

    override fun sendWindowSetSlot(player: Player, containerId: Int, slot: Int, itemStack: ItemStack?, stateId: Int) {
        when {
            versionId >= 11701 -> {
                player.postPacket(
                    PacketPlayOutSetSlot::class.java.unsafeInstance(),
                    "containerId" to containerId,
                    "slot" to slot,
                    "itemStack" to itemStack.asNMSCopy(),
                    "stateId" to stateId,
                )
            }

            else -> {
                player.sendPacketBlocking(PacketPlayOutSetSlot(containerId, slot, itemStack.asNMSCopy()))
            }
        }
    }

    override fun sendWindowSetSlots(player: Player, containerId: Int, items: Map<Int, ItemStack?>) {
        val packets = items.map { (slot, itemStack) ->
            when {
                versionId >= 11701 -> {
                    PacketPlayOutSetSlot::class.java.unsafeInstance().also {
                        it.setProperty("containerId", containerId)
                        it.setProperty("slot", slot)
                        it.setProperty("itemStack", itemStack.asNMSCopy())
                        it.setProperty("stateId", 1)
                    }
                }

                else -> {
                    PacketPlayOutSetSlot(containerId, slot, itemStack.asNMSCopy())
                }
            }
        }
        player.sendBundlePacketBlocking(packets)
    }

    override fun sendWindowUpdateData(player: Player, containerId: Int, property: WindowProperty, value: Int) {
        PacketPlayOutWindowData(containerId, property.index, value).let {
            player.sendPacketBlocking(it)
        }
    }

    override fun asCraftMirror(itemStack: Any): ItemStack {
        return CraftItemStack.asCraftMirror(itemStack as net.minecraft.server.v1_16_R3.ItemStack?) as ItemStack
    }

    override fun getContainerId(player: Player): Int {
        player as CraftPlayer
        return if (isUniversal) {
            player.handle.getProperty<Container>("containerMenu")!!.getProperty<Int>("containerId")!!
        } else {
            player.handle.activeContainer.windowId
        }
    }

    override fun getActiveContainerId(player: Player): Int {
        if (!player.isOnline) return -1
        try {
            player as CraftPlayer
            return if (isUniversal) {
                // 1.17+
                player.handle.getProperty<Container>("containerMenu")?.getProperty<Int>("containerId") ?: -1
            } else {
                // 旧版本
                player.handle.activeContainer?.windowId ?: -1
            }
        } catch (e: Exception) {
            // 如果发生异常，返回-1表示无效容器
            e.printStackTrace()
            return -1
        }
    }

    private fun ItemStack?.asNMSCopy(): net.minecraft.server.v1_16_R3.ItemStack {
        return CraftItemStack.asNMSCopy(this)
    }

    private fun List<ItemStack?>.asNMSCopy(): List<net.minecraft.server.v1_16_R3.ItemStack> {
        return map { it.asNMSCopy() }
    }

}