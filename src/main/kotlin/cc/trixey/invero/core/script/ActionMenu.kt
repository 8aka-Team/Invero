package cc.trixey.invero.core.script

import cc.trixey.invero.common.Invero
import cc.trixey.invero.common.util.parseMappedArguments
import cc.trixey.invero.core.BaseMenu
import cc.trixey.invero.core.script.loader.InveroKetherParser
import org.bukkit.Bukkit
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.chat.component
import taboolib.module.kether.*

/**
 * Invero
 * cc.trixey.invero.expansion.script.menu.ActionsMenu
 *
 * @author Arasple
 * @since 2023/1/19 17:37
 */
object ActionMenu {

    /*
    - menu title set "xxx"
    - menu title pause
    - menu title resume
    - menu title update
    - menu close
    - menu open [menuId] for [player] with [customArguments]
    - menu switch [menuId] for [player] with [customArguments]
     */
    @InveroKetherParser(["menu"])
    fun parserMenu() = scriptParser {
        when (it.expects("title", "close", "open", "open_ctx", "switch")) {
            "title" -> handlerMenuTitle(it)
            "close" -> actionNow { session()?.menu?.close(player()) }
            "open" -> handlerMenuOpen(it)
            "open_ctx", "switch" -> handlerMenuOpen(it, true)
            else -> error("Unknown case")
        }
    }

    private fun handlerMenuTitle(it: QuestReader) =
        when (it.expects("get", "set", "to", "pause", "resume", "update")) {
            "get" -> actionNow { session()?.window?.title }

            "set", "to" -> {
                val input = it.nextParsedAction()
                actionFuture { future ->
                    val session = session()
                    newFrame(input).run<String>().thenApply { title ->
                        if (session != null) {
                            title
                                .component()
                                .build {
                                    transform {
                                        session.parse(it)
                                    }
                                }
                                .toLegacyText()
                                .let { session.window.title = it }
                        }

                        future.complete(title)
                    }
                }
            }

            "pause" -> actionNow { session()?.pauseAnimatedTitle() }
            "resume" -> actionNow { session()?.resumeAnimatedTitle() }
            "update" -> actionNow { session()?.apply { (menu as BaseMenu).updateTitle(this) } }

            else -> error("Unknown case")
        }


    /*
    menu open <menuId> for [player] with [customArguments]
     */
    private fun handlerMenuOpen(reader: QuestReader, reserveContext: Boolean = false): ScriptAction<Any?> {
        val menuId = reader.nextParsedAction()
        var player: ParsedAction<*>? = null
        var customArguments: ParsedAction<*>? = null

        repeat(2) {
            reader.mark()
            try {
                when (reader.expects("for", "with")) {
                    "for" -> player = reader.nextParsedAction()
                    "with" -> customArguments = reader.nextParsedAction()
                }
            } catch (_: Throwable) {
                reader.reset()
            }
        }

        return actionFuture { future ->
            newFrame(menuId).run<Any>().thenApply { id ->
                val menu = Invero.API.getMenuManager().getMenu(id.toString()) ?: error("Not found menu with id $id")
                val viewer = player?.let {
                    newFrame(it).run<Any>().getNow(null)
                        ?.let { Bukkit.getPlayerExact(it.toString()) }
                } ?: player()
                val arguments = buildMap {
                    if (reserveContext) session()?.getVariables()?.let {
                        putAll(it)
                    }
                    customArguments?.let {
                        putAll(newFrame(it).run<Any>().getNow("").toString().parseMappedArguments())
                    }
                }

                menu.open(viewer, arguments)
            }.get().let { future.complete(it) }
        }
    }

}