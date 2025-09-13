package cc.trixey.invero.core.script.override

import cc.trixey.invero.core.Context
import cc.trixey.invero.core.script.contextVar
import cc.trixey.invero.core.script.loader.InveroKetherParser
import cc.trixey.invero.core.script.parse
import cc.trixey.invero.core.script.player
import cc.trixey.invero.common.message.sendFormattedAdventureComponent
import cc.trixey.invero.common.message.sendFormattedTabooComponent
import taboolib.common.platform.function.onlinePlayers
import taboolib.module.kether.combinationParser
import taboolib.platform.util.sendActionBar

/**
 * Invero
 * cc.trixey.invero.core.script.kether.override.Actions
 *
 * @author Arasple
 * @since 2023/3/3 18:56
 */
@InveroKetherParser(["tell", "send", "message", "msg"], tags = ["kether-ext"])
fun actionTell() = combinationParser {
    it.group(
        text(),
    ).apply(it) { message ->
        now {
            val context = contextVar<Context?>("@context")?.variables ?: variables().toMap()
            val player = player()

            message.sendFormattedAdventureComponent(player, context)
        }
    }
}

@InveroKetherParser(["minimessage", "mmessage", "mmsg"], tags = ["kether-ext"])
fun actionTellMiniMessage() = combinationParser {
    it.group(
        text(),
    ).apply(it) { message ->
        now {
            val context = contextVar<Context?>("@context")?.variables ?: variables().toMap()
            val player = player()

            message.sendFormattedAdventureComponent(player, context)
        }
    }
}

@InveroKetherParser(["fluentmessage", "fmessage", "fmsg"], tags = ["kether-ext"])
fun actionTellFluentMessage() = combinationParser {
    it.group(
        text(),
    ).apply(it) { message ->
        now {
            val context = contextVar<Context?>("@context")?.variables ?: variables().toMap()
            val player = player()

            message.sendFormattedTabooComponent(player, context)
        }
    }
}


@InveroKetherParser(["actionbar"], tags = ["kether-ext"])
fun actionActionBar() = combinationParser {
    it.group(text()).apply(it) { str ->
        now { player().sendActionBar(parse(str)) }
    }
}

@InveroKetherParser(["broadcast", "bc"], tags = ["kether-ext"])
fun actionBroadcast() = combinationParser {
    it.group(text()).apply(it) { str ->
        now { onlinePlayers().forEach { p -> p.sendMessage(parse(str)) } }
    }
}


@InveroKetherParser(["title"], tags = ["kether-ext"])
fun actionTitle() = combinationParser {
    it.group(
        text(),
        command("subtitle", then = text()).option(),
        command("by", "with", then = int().and(int(), int())).option().defaultsTo(Triple(0, 20, 0))
    ).apply(it) { t1, t2, time ->
        val (i, s, o) = time
        now { player().sendTitle(parse(t1), t2?.let { i -> parse(i) }, i, s, o) }
    }
}

@InveroKetherParser(["subtitle"], tags = ["kether-ext"])
fun actionSubtitle() = combinationParser {
    it.group(
        text(),
        command("by", "with", then = int().and(int(), int())).option().defaultsTo(Triple(0, 20, 0))
    ).apply(it) { text, time ->
        val (i, s, o) = time
        now { player().sendTitle("§r", text.replace("@sender", player().name), i, s, o) }
    }
}