package cc.trixey.invero.ui.bukkit

import cc.trixey.invero.ui.bukkit.api.dsl.viewer
import cc.trixey.invero.ui.bukkit.api.findWindow
import cc.trixey.invero.ui.bukkit.nms.persistContainerId
import cc.trixey.invero.ui.bukkit.nms.sendCancelCoursor
import cc.trixey.invero.ui.bukkit.util.copyUIMarked
import cc.trixey.invero.ui.common.event.ClickType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.module.nms.MinecraftVersion.isUniversal
import taboolib.module.nms.PacketReceiveEvent
import taboolib.platform.event.PlayerJumpEvent
import taboolib.platform.util.hasMeta
import taboolib.platform.util.removeMeta

/**
 * Invero
 * cc.trixey.invero.ui.bukkit.Listener
 *
 * @author Arasple
 * @since 2023/1/12 18:03
 */
object Listener {

    private val FIELD_CONTAINER_ID = if (isUniversal) "containerId" else "id"
    private val FILEDS_WINDOW_CLICK =
        if (isUniversal) arrayOf("containerId", "slotNum", "buttonNum", "clickType", "carriedItem")
        else arrayOf("a", "slot", "button", "shift", "chemdah")

    @Awake(LifeCycle.ACTIVE)
    fun init() {
        // init NBT handle
        ItemStack(Material.DIAMOND).copyUIMarked("Invero", 0)
    }

    @SubscribeEvent
    fun e(e: InventoryClickEvent) = e.delegatedEvent {
        when (e.action) {
            InventoryAction.MOVE_TO_OTHER_INVENTORY -> handleItemsMove(e)
            InventoryAction.COLLECT_TO_CURSOR -> handleItemsCollect(e)
            else -> handleClick(e)
        }
    }

    @SubscribeEvent
    fun e(e: InventoryDragEvent) = e.delegatedEvent { handleDrag(e) }

    @SubscribeEvent
    fun e(e: InventoryOpenEvent) {
        if (!e.delegatedEvent { handleOpenEvent(e) }) {
            val holder = e.inventory.holder?.javaClass?.simpleName

            if (holder?.contains("Menu") != true) {
                (e.player as Player).updateInventory()
            }
        }
    }

    @SubscribeEvent
    fun e(e: InventoryCloseEvent) = e.delegatedEvent { handleCloseEvent(e) }

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        try {
            e.player.viewer.viewingWindow()?.close(false, updateInventory = false)
        } catch (_: Throwable) {

        }
    }

    @SubscribeEvent
    fun e(e: EntityPickupItemEvent) {
        (e.entity as? Player)?.name?.let { name ->
            if (findWindow(name) != null && e.entity.hasMeta("invero_menu_viewing")) {
//                e.entity.sendMessage("§cUnauthorized to pick up items in the menu. ${e.entity.hasMeta("invero_menu_viewing")}")
                e.isCancelled = true
            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerJumpEvent) {
        val player = e.player
        player.removeMeta("invero_menu_viewing")
    }

    @SubscribeEvent
    fun e(e: PlayerMoveEvent) {
        val player = e.player
        if (player.hasMeta("invero_menu_viewing")) {
            player.removeMeta("invero_menu_viewing")
        }
    }

    @SubscribeEvent
    fun e(e: PlayerTeleportEvent) {
        val player = e.player
        if (player.hasMeta("invero_menu_viewing")) {
            player.removeMeta("invero_menu_viewing")
        }
    }

    @SubscribeEvent
    fun ex(e: PlayerChangedWorldEvent) {
        val player = e.player
        if (player.hasMeta("invero_menu_viewing")) {
            player.removeMeta("invero_menu_viewing")
        }
    }

    private fun InventoryEvent.delegatedEvent(block: InventoryVanilla.() -> Unit): Boolean {
        val topInventory = view.invokeMethod<Inventory>("getTopInventory") ?: return false
        val holder = topInventory.holder
        if (holder is InventoryVanilla.Holder) {
            (holder.window.inventory as InventoryVanilla).block()
            return true
        }
        return false
    }

    @SubscribeEvent
    fun e(e: PacketReceiveEvent) {
        val player = e.player
        val viewer = player.viewer
        val packet = e.packet

        when (packet.name) {
            "PacketPlayInCloseWindow" -> {
                val id = packet.read<Int>(FIELD_CONTAINER_ID) ?: return
                if (id == persistContainerId) {
                    val window = viewer.viewingPacketWindow() ?: return
                    submit { window.close(doCloseInventory = false, updateInventory = true) }
                }
            }

            "PacketPlayInWindowClick", "ServerboundContainerClickPacket" -> {
                val inventory = viewer.viewingWindow()?.inventory ?: return

                if (inventory is InventoryVanilla) {
                    player.sendCancelCoursor()
                    submit { inventory.updatePlayerInventory() }
                    return
                } else {
                    packet.read<Int>(FILEDS_WINDOW_CLICK[0]).let { if (it != persistContainerId) return }
                    inventory as InventoryPacket
                }

                val rawSlot = packet.read<Number>(FILEDS_WINDOW_CLICK[1])?.toInt() ?: return
                val button = packet.read<Number>(FILEDS_WINDOW_CLICK[2])?.toInt() ?: return
                val mode = ClickType.Mode.valueOf(packet.read<Any>(FILEDS_WINDOW_CLICK[3]).toString())
                val type = ClickType.find(mode, button, rawSlot) ?: return


                if (rawSlot >= 0) {
                    player.sendCancelCoursor()
                    inventory.update(rawSlot)
                }

                submit {
                    inventory.handleClickEvent(rawSlot, type)
                }
            }
        }
    }

    /*
    ENTITIY_SAFETY
     */
    @SubscribeEvent
    fun e(e: PlayerDeathEvent) = e.entity.windowClosure()

    @SubscribeEvent
    fun e(e: PlayerChangedWorldEvent) = e.player.windowClosure()

    private fun PlayerViewer.viewingPacketWindow(): BukkitWindow? {
        return viewingWindow()?.let { if (it.inventory is InventoryPacket) it else null }
    }

    private fun PlayerViewer.viewingWindow(): BukkitWindow? {
        return findWindow(name)
    }

    private fun Player.windowClosure() {
        findWindow(name)?.close(doCloseInventory = false, updateInventory = true)
    }
}