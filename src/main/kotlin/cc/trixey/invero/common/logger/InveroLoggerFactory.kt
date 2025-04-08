package cc.trixey.invero.common.logger

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.NOPLogger
import org.slf4j.spi.LoggerFactoryBinder
import taboolib.common.platform.function.console

/**
 * 自定义的SLF4J LoggerFactoryBinder实现
 * 用于在插件初始化时替代默认的NOP日志实现
 */
@Suppress("unused")
class StaticLoggerBinder private constructor() : LoggerFactoryBinder {

    companion object {
        /**
         * The unique instance of this class.
         */
        @JvmField
        val SINGLETON = StaticLoggerBinder()

        /**
         * Return the singleton of this class.
         */
        @JvmStatic
        fun getSingleton(): StaticLoggerBinder {
            return SINGLETON
        }
    }

    private val loggerFactoryClassStr = InveroLoggerFactory::class.java.name
    private val loggerFactory = InveroLoggerFactory()

    override fun getLoggerFactory(): ILoggerFactory {
        return loggerFactory
    }

    override fun getLoggerFactoryClassStr(): String {
        return loggerFactoryClassStr
    }
}

/**
 * 自定义的SLF4J ILoggerFactory实现
 */
class InveroLoggerFactory : ILoggerFactory {
    private val loggers = mutableMapOf<String, Logger>()

    override fun getLogger(name: String): Logger {
        return loggers.getOrPut(name) { InveroLogger(name) }
    }
}

/**
 * 自定义的SLF4J Logger实现
 * 使用TabooLib的控制台进行日志输出
 */
class InveroLogger(private val name: String) : Logger {
    
    override fun getName(): String = name

    override fun isTraceEnabled(): Boolean = false
    override fun isTraceEnabled(marker: Marker?): Boolean = false
    override fun trace(msg: String?) {}
    override fun trace(format: String?, arg: Any?) {}
    override fun trace(format: String?, arg1: Any?, arg2: Any?) {}
    override fun trace(format: String?, vararg arguments: Any?) {}
    override fun trace(msg: String?, t: Throwable?) {}
    override fun trace(marker: Marker?, msg: String?) {}
    override fun trace(marker: Marker?, format: String?, arg: Any?) {}
    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {}
    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {}
    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {}

    override fun isDebugEnabled(): Boolean = false
    override fun isDebugEnabled(marker: Marker?): Boolean = false
    override fun debug(msg: String?) {}
    override fun debug(format: String?, arg: Any?) {}
    override fun debug(format: String?, arg1: Any?, arg2: Any?) {}
    override fun debug(format: String?, vararg arguments: Any?) {}
    override fun debug(msg: String?, t: Throwable?) {}
    override fun debug(marker: Marker?, msg: String?) {}
    override fun debug(marker: Marker?, format: String?, arg: Any?) {}
    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {}
    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {}
    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {}

    override fun isInfoEnabled(): Boolean = true
    override fun isInfoEnabled(marker: Marker?): Boolean = true
    override fun info(msg: String?) {
        if (msg != null) console().sendMessage("§b[Invero] $msg")
    }
    override fun info(format: String?, arg: Any?) {
        if (format != null) console().sendMessage("§b[Invero] $format")
    }
    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (format != null) console().sendMessage("§b[Invero] $format")
    }
    override fun info(format: String?, vararg arguments: Any?) {
        if (format != null) console().sendMessage("§b[Invero] $format")
    }
    override fun info(msg: String?, t: Throwable?) {
        if (msg != null) console().sendMessage("§b[Invero] $msg")
        t?.printStackTrace()
    }
    override fun info(marker: Marker?, msg: String?) {
        if (msg != null) console().sendMessage("§b[Invero] $msg")
    }
    override fun info(marker: Marker?, format: String?, arg: Any?) {
        if (format != null) console().sendMessage("§b[Invero] $format")
    }
    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (format != null) console().sendMessage("§b[Invero] $format")
    }
    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (format != null) console().sendMessage("§b[Invero] $format")
    }
    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        if (msg != null) console().sendMessage("§b[Invero] $msg")
        t?.printStackTrace()
    }

    override fun isWarnEnabled(): Boolean = true
    override fun isWarnEnabled(marker: Marker?): Boolean = true
    override fun warn(msg: String?) {
        if (msg != null) console().sendMessage("§e[Invero] $msg")
    }
    override fun warn(format: String?, arg: Any?) {
        if (format != null) console().sendMessage("§e[Invero] $format")
    }
    override fun warn(format: String?, vararg arguments: Any?) {
        if (format != null) console().sendMessage("§e[Invero] $format")
    }
    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (format != null) console().sendMessage("§e[Invero] $format")
    }
    override fun warn(msg: String?, t: Throwable?) {
        if (msg != null) console().sendMessage("§e[Invero] $msg")
        t?.printStackTrace()
    }
    override fun warn(marker: Marker?, msg: String?) {
        if (msg != null) console().sendMessage("§e[Invero] $msg")
    }
    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        if (format != null) console().sendMessage("§e[Invero] $format")
    }
    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (format != null) console().sendMessage("§e[Invero] $format")
    }
    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (format != null) console().sendMessage("§e[Invero] $format")
    }
    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        if (msg != null) console().sendMessage("§e[Invero] $msg")
        t?.printStackTrace()
    }

    override fun isErrorEnabled(): Boolean = true
    override fun isErrorEnabled(marker: Marker?): Boolean = true
    override fun error(msg: String?) {
        if (msg != null) console().sendMessage("§c[Invero] $msg")
    }
    override fun error(format: String?, arg: Any?) {
        if (format != null) console().sendMessage("§c[Invero] $format")
    }
    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (format != null) console().sendMessage("§c[Invero] $format")
    }
    override fun error(format: String?, vararg arguments: Any?) {
        if (format != null) console().sendMessage("§c[Invero] $format")
    }
    override fun error(msg: String?, t: Throwable?) {
        if (msg != null) console().sendMessage("§c[Invero] $msg")
        t?.printStackTrace()
    }
    override fun error(marker: Marker?, msg: String?) {
        if (msg != null) console().sendMessage("§c[Invero] $msg")
    }
    override fun error(marker: Marker?, format: String?, arg: Any?) {
        if (format != null) console().sendMessage("§c[Invero] $format")
    }
    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (format != null) console().sendMessage("§c[Invero] $format")
    }
    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (format != null) console().sendMessage("§c[Invero] $format")
    }
    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        if (msg != null) console().sendMessage("§c[Invero] $msg")
        t?.printStackTrace()
    }
} 