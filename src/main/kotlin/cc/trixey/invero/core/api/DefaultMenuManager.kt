package cc.trixey.invero.core.api

import cc.trixey.invero.common.Invero
import cc.trixey.invero.common.Menu
import cc.trixey.invero.common.api.InveroMenuManager
import cc.trixey.invero.common.api.InveroSettings
import cc.trixey.invero.common.api.SerializeResult
import cc.trixey.invero.common.api.SerializeResult.State.*
import cc.trixey.invero.common.util.alert
import cc.trixey.invero.common.util.findInJar
import cc.trixey.invero.common.util.prettyPrint
import cc.trixey.invero.core.AgentPanel
import cc.trixey.invero.core.BaseMenu
import cc.trixey.invero.core.action.*
import cc.trixey.invero.core.panel.PanelGenerator
import cc.trixey.invero.core.panel.PanelPaged
import cc.trixey.invero.core.panel.PanelScroll
import cc.trixey.invero.core.panel.PanelStandard
import cc.trixey.invero.core.serialize.BaseMenuSerializer
import cc.trixey.invero.core.serialize.hocon.PatchedLoader
import cc.trixey.invero.core.util.session
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.io.newFile
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getJarFile
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common5.FileWatcher
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import taboolib.module.lang.sendLang
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.sendLang
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Invero
 * cc.trixey.invero.core.api.DefaultMenuManager
 *
 * @author Arasple
 * @since 2023/2/1 17:17
 */
class DefaultMenuManager : InveroMenuManager {

    @OptIn(ExperimentalSerializationApi::class)
    private val module = SerializersModule {

        polymorphic(Menu::class) {
            subclass(BaseMenu::class)
        }

        polymorphic(AgentPanel::class) {
            subclass(PanelStandard::class)
            subclass(PanelGenerator::class)
            subclass(PanelPaged::class)
            subclass(PanelScroll::class)
            subclass(PanelGenerator::class)
        }

        polymorphic(Action::class) {
            subclass(ActionKether::class)
            subclass(ConditionIf::class)
            subclass(ConditionAll::class)
            subclass(ConditionAny::class)
            subclass(ConditionNone::class)
            subclass(ConditionIfNot::class)
            subclass(ConditionCase::class)
            subclass(StructureActionKether::class)
            subclass(FunctionalActionCatcher::class)
            subclass(FunctionalActionCatchers::class)
            subclass(NetesedAction::class)
        }

    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        serializersModule = module
        explicitNulls = false
    }

    private val menus = ConcurrentHashMap<String, BaseMenu>()

    override fun getMenu(id: String, ignoreCase: Boolean): BaseMenu? {
        return menus.entries.find { it.key.equals(id, ignoreCase) }?.value
    }

    override fun getMenus(): List<BaseMenu> {
        return menus.values.toList()
    }

    override fun deserialize(workspace: File): List<SerializeResult> {
        val matcher = InveroSettings.fileFilter.toRegex()
        val results = mutableListOf<SerializeResult>()
        val configurations = workspace
            .walk()
            .filter { it.name.matches(matcher) }
            .mapNotNull {
                runCatching {
                    val conf = PatchedLoader.loadFromFile(it)
                    val keys = conf.getKeys(false)
                    if (menuDeclarations.any { it in keys }) conf else null
                }.onFailure { failure ->
                    results += SerializeResult(
                        file = it,
                        state = FAILURE_FILE,
                        throwable = failure
                    )
                }.getOrNull()
            }

        configurations.forEach { conf ->
            val file = conf.file ?: error("No valid file of configuration")
            val menu = runCatching { deserializeToMenu(conf, conf.name) }.onFailure {
                results += SerializeResult(
                    file = file,
                    state = FAILURE_MENU,
                    throwable = it
                )
            }.getOrNull()

            if (menu != null) {
                if (getMenu(conf.name, true) != null) {
                    results += SerializeResult(
                        file = file,
                        state = FAILURE_DUPLICATED
                    )
                } else {
                    menus[conf.name] = menu

                    results += SerializeResult(
                        menu,
                        file,
                        SUCCESS
                    )
                }
            }
        }

        return results
    }

    override fun reload(receiver: CommandSender) {
        // mark time start
        val start = System.currentTimeMillis()
        // unregister all menus
        if (menus.isNotEmpty()) {
            onlinePlayers.forEach { player -> player.session?.menu?.close(player) }
            menus.clear()
            menus.values.forEach { it.unregister() }
        }
        // init workspaces
        val workspaces = initWorkspaces()

        if (workspaces.isEmpty()) {
            receiver.sendLang("menu-loader-workspace-empty")
        } else {
            receiver.sendLang("menu-loader-workspace-inited", workspaces.size)
            workspaces
                .flatMap { deserialize(it) }
                .forEach {
                    val file = it.file
                    when (it.state) {
                        FAILURE_FILE -> receiver.sendLang("menu-loader-file-errored", file.name)
                        FAILURE_MENU -> receiver.sendLang("menu-loader-menu-errored", file.name)
                        FAILURE_DUPLICATED -> receiver.sendLang("menu-loader-menu-duplicate", file.name)
                        SUCCESS -> if (InveroSettings.fileListener) registerListener(file, it.menu as BaseMenu)
                    }
                    it.print()
                }
            if (menus.isNotEmpty()) {
                val took = (System.currentTimeMillis() - start).div(1000.0)
                receiver.sendLang("menu-loader-menu-finished", menus.size, took)
            }

            menus.values.forEach { it.register() }
        }
    }

    private fun registerListener(file: File, menu: BaseMenu) {
        val menuId = menu.id!!

        FileWatcher.INSTANCE.addSimpleListener(file) {
            submitAsync {
                if (!file.exists() || !menus.containsKey(menuId)) {
                    FileWatcher.INSTANCE.removeListener(file)
                } else runCatching {
                    deserializeToMenu(PatchedLoader.loadFromFile(file), menuId)
                }.onFailure {
                    it.prettyPrint()
                    console().sendLang("menu-loader-auto-reload-errored", menuId)
                }.getOrNull()?.let { loaded ->
                    val viewers = onlinePlayers.filter { it.session?.menu?.id == menuId }
                    // replace in memory
                    menus[menuId]?.apply {
                        viewers.forEach { close(it, false, closeInventory = false) }
                        unregister()
                    }
                    menus[menuId] = loaded
                    alert { loaded.register() }
                    submit {
                        console().sendLang("menu-loader-auto-reload-successed", menuId)
                        viewers.forEach {
                            loaded.open(player = it, vars = it.session?.getVariables() ?: emptyMap())
                        }
                    }
                }
            }
        }
    }

    override fun deserializeToMenu(configuration: Configuration, name: String?): BaseMenu {
        configuration.changeType(Type.JSON)
        return json
            .decodeFromString(BaseMenuSerializer, configuration.saveToString())
            .also { if (name != null && it.id == null) it.id = name }
    }

    override fun serializeToJson(menu: Menu): String {
        return json.encodeToString(value = menu)
    }

    override fun <T> getJsonSerializer(): T {
        @Suppress("UNCHECKED_CAST")
        return json as T
    }

    private val menuDeclarations = mutableSetOf("menu", "title")

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            PlatformFactory.registerAPI<InveroMenuManager>(DefaultMenuManager())

            submitAsync(delay = 15L) {
                Invero.API.getMenuManager().reload()
            }
        }

        fun initWorkspaces(): List<File> {
            val list = ArrayList<File>()

            for (path in InveroSettings.workspaces) {
                val file = File(path)
                // release defaults if not exist
                if (!file.exists()) { 
                    releaseWorkspace(file)
                }
                if (file.isDirectory) list.add(file)
            }

            return list
        }

        /**
         * 复制默认工作空间内文件到默认工作空间
         */
        fun releaseWorkspace(folder: File) {
            findInJar(getJarFile()) {
                !it.isDirectory && it.name.startsWith("default/")
            }.forEach {
                newFile(File(folder, it.first.name.substringAfter('/'))).writeBytes(it.second.readBytes())
            }
        }

    }

}