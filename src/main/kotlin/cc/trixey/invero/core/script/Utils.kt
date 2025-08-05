package cc.trixey.invero.core.script

import cc.trixey.invero.common.Object
import cc.trixey.invero.core.Session
import cc.trixey.invero.core.icon.IconElement
import cc.trixey.invero.core.util.session
import cc.trixey.invero.common.message.translateFormattedMessage
import cc.trixey.invero.ui.bukkit.PanelContainer
import cc.trixey.invero.ui.common.Panel
import cc.trixey.invero.ui.common.panel.ElementalPanel
import cc.trixey.invero.ui.common.util.getSiblings
import org.bukkit.entity.Player
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.deepVars
import taboolib.module.kether.script

/**
 * Invero
 * cc.trixey.invero.expansion.script.Helper
 *
 * @author Arasple
 * @since 2023/1/19 17:37
 */
fun ScriptFrame.player(): Player {
    return script().sender?.castSafely<Player>() ?: error("No player found.")
}

fun ScriptFrame.getRecursivePanels(): List<Panel> {
    return session()?.window?.getPanelsRecursively() ?: emptyList()
}

fun ScriptFrame.getRootPanels(): List<Panel> {
    return session()?.window?.panels ?: emptyList()
}

fun <T : Panel> ScriptFrame.findPanelAt(indexs: List<Int>): T? {
    val iterator = indexs.iterator()
    var panel: Any? = session()?.window
    while (iterator.hasNext() && panel is PanelContainer) {
        val index = iterator.next()
        panel = panel.panels[index]
    }
    @Suppress("UNCHECKED_CAST")
    return panel as T?
}

fun ScriptFrame.iconElementBy(byName: String?, bySlot: Int?, referedPanel: ElementalPanel?): IconElement {
    return (referedPanel ?: findNearstPanelRecursively())?.findIconElement(byName, bySlot) ?: selfIcon()
}

fun ElementalPanel.findIconElement(byName: String?, bySlot: Int?): IconElement? {
    return if (byName != null) {
        elements.value.keys.find { (it as IconElement).icon.id == byName } as IconElement?
    } else if (bySlot != null) {
        val position = scale.convertToPosition(bySlot)
        elements.value.entries.find { it.value.contains(position) }?.key as IconElement?
    } else {
        null
    }
}

fun ScriptFrame.selfIcon(): IconElement {
    return contextVar<IconElement>("@icon") ?: error("Nulled icon context")
}

fun ScriptFrame.selfSourceObject(): Object {
    return contextVar<Object>("@element") ?: error("Nulled sourceObject context")
}

inline fun <reified T : Panel> ScriptFrame.selfPanel(): T {
    return contextVar<T>("@panel") ?: findNearstPanel() ?: error("Nulled panel context")
}

inline fun <reified T : Panel> ScriptFrame.findNearstPanel(): T? {
    val siblings = contextVar<T>("@panel")?.getSiblings()?.toMutableList()
    return (siblings ?: session()?.window?.panels)?.filterIsInstance<T>()?.firstOrNull()
}

inline fun <reified T : Panel> ScriptFrame.findNearstPanelRecursively(): T? {
    val contextPanel = contextVar<Panel>("@panel")
    val parentPanel = contextPanel?.parent as? T

    return (contextPanel as? T ?: parentPanel) ?: getRecursivePanels().filterIsInstance<T>().firstOrNull()
}

fun <T> ScriptFrame.contextVar(key: String): T? {
    @Suppress("UNCHECKED_CAST")
    return session()?.getVariable(key) as? T ?: scriptVar(key)
}

fun <T> ScriptFrame.scriptVar(key: String): T? {
    return variables()?.getOrNull<T>(key)
}

fun ScriptFrame.session(): Session? {
    return player().session
}

fun ScriptFrame.parse(string: String, skipComp: Boolean = false): String {
    val session = session()
    if (session != null) {
        return session.parse(string)
    } else {
        return string.translateFormattedMessage(player(), deepVars(), skipComp)
    }
}