package cc.trixey.invero.ui.bukkit.panel

import cc.trixey.invero.ui.bukkit.PanelContainer
import cc.trixey.invero.ui.bukkit.ProxyBukkitInventory
import cc.trixey.invero.ui.bukkit.util.clickType
import cc.trixey.invero.ui.bukkit.util.reachedMaxStackSize
import cc.trixey.invero.ui.common.Pos
import cc.trixey.invero.ui.common.Scale
import cc.trixey.invero.ui.common.event.ClickType
import cc.trixey.invero.ui.common.panel.ElementalPanel
import cc.trixey.invero.ui.common.panel.IOPanel
import cc.trixey.invero.ui.common.panel.PanelWeight
import cc.trixey.invero.ui.common.util.locatingAbsoluteSlot
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.platform.util.isAir
import java.util.concurrent.ConcurrentHashMap

/**
 * Invero
 * cc.trixey.invero.ui.bukkit.panel.CraftingPanel
 *
 * @author Arasple
 * @since 2023/2/10 17:00
 */
class CraftingPanel(
    parent: PanelContainer,
    weight: PanelWeight,
    scale: Scale,
    locate: Pos
) : StandardPanel(parent, weight, scale, locate), ElementalPanel, IOPanel {

    /**
     * Bukkit 容器
     */
    override val inventory: ProxyBukkitInventory
        get() = super.inventory as ProxyBukkitInventory

    /**
     * 允许物品交互的槽位
     */
    override val freeSlots by lazy {
        mutableSetOf<Int>().apply {
            this += (0 until scale.size)
            this -= elements.occupiedPositions().map { it.slot }.toSet()
        }
    }

    /**
     * 物品改变回调函数
     */
    override var callback: () -> Unit = {}

    /**
     * 物品存储
     * <相对槽位> : <物品>
     *
     * 相对槽位应属于 freeSlots
     */
    val storage = ConcurrentHashMap<Int, ItemStack>()

    /**
     * 点击交互事件处理
     */
    override fun handleClick(pos: Pos, clickType: ClickType, e: InventoryClickEvent?): Boolean {
        require(e != null)

        if (!super.handleClick(pos, clickType, e) && pos.slot in freeSlots) {
            // cancel event & run callbacks
            e.isCancelled = false
            // update storage
            submit {
                (freeSlots + storage.keys).distinct().forEach {
                    inventory[locatingAbsoluteSlot(it)].storeAt(it)
                }
            }
            runCallback()
            return true
        }
        return false
    }

    /**
     * 拖拽分配交互事件处理
     */
    override fun handleDrag(pos: List<Pos>, e: InventoryDragEvent): Boolean {
        // locked slots
        if (pos.any { it.slot !in freeSlots }) return false

        // 放行原生拖拽行为
        e.isCancelled = false

        // 事件结束后统一回读 GUI 槽位，确保存储与界面一致
        // 支持拖入与取出
        submit {
            (freeSlots + storage.keys).distinct().forEach {
                inventory[locatingAbsoluteSlot(it)].storeAt(it)
            }
            runCallback()
        }
        return true
    }

    /**
     * 快捷移动物品交互事件处理
     */
    override fun handleItemsMove(pos: Pos, e: InventoryClickEvent): Boolean {
        val slot = pos.slot
        if (slot !in freeSlots) return handleClick(pos, e.clickType, e)

        e.isCancelled = false

        submit {
            inventory[locatingAbsoluteSlot(pos)].storeAt(slot)
            runCallback()
        }
        return true
    }

    /**
     * 删除物品
     */
    override fun delete(slot: Int) {
        storeDel(slot)
        renderStorage()
    }

    /**
     * 设置物品
     */
    fun set(slot: Int, itemStack: ItemStack?) {
        itemStack.storeAt(slot)
        renderStorage()
    }

    /**
     * 插入物品（堆叠）
     * 返回剩余数量
     */
    fun insert(itemStack: ItemStack): Int {
        var amount = itemStack.amount

        storage.forEach { (_, stored) ->
            if (stored.isSimilar(itemStack)) {
                while (!stored.reachedMaxStackSize() && amount > 0) {
                    stored.amount++
                    amount--
                }
            }
        }

        itemStack.amount = amount

        while (amount > 0 && freeSlots.any { storage[it].isAir }) {
            val index = freeSlots.first { storage[it].isAir }
            storage[index] = itemStack
            amount -= itemStack.amount
        }

        renderStorage()
        runCallback()
        return amount
    }

    /**
     * 取得储存物品（相对槽位）
     */
    fun getStorageItem(slot: Int) = storage.getOrDefault(slot, null)

    /**
     * 重新渲染存储空间的所有物品
     */
    override fun renderStorage() {
        storage.forEach { (index, item) -> inventory[locatingAbsoluteSlot(index)] = item }
        freeSlots
            .filterNot { it in storage.keys }
            .forEach { inventory[locatingAbsoluteSlot(it)] = null }
    }

    /*
    PRIVATE FUNCTIONS
     */

    private fun ItemStack?.storeAt(slot: Int) {
        if (this == null) storeDel(slot)
        else storage[slot] = this
        runCallback()
    }

    private fun storeDel(slot: Int) {
        storage.remove(slot)
    }

}