package org.slf4j.impl

import cc.trixey.invero.common.logger.InveroLoggerFactory
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.LoggerFactoryBinder
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.MarkerFactoryBinder
import taboolib.common.env.RuntimeDependency

/**
 * SLF4J实现类
 * 使用RuntimeDependency注解简化SLF4J依赖管理
 * 通过单一文件替代原本的三个独立文件
 */
@RuntimeDependency("org.slf4j:slf4j-api:1.7.36")
class StaticLoggerBinder private constructor() : LoggerFactoryBinder {

    companion object {
        @JvmField
        val REQUESTED_API_VERSION = "1.7.36" 

        @JvmField
        val SINGLETON = StaticLoggerBinder()

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
 * SLF4J标记绑定
 * 使用RuntimeDependency注解简化SLF4J依赖管理
 */
@RuntimeDependency("org.slf4j:slf4j-api:1.7.36")
class StaticMarkerBinder private constructor() : MarkerFactoryBinder {

    companion object {
        @JvmField
        val SINGLETON = StaticMarkerBinder()

        @JvmStatic
        fun getSingleton(): StaticMarkerBinder {
            return SINGLETON
        }
    }

    private val markerFactory = BasicMarkerFactory()

    override fun getMarkerFactory(): IMarkerFactory {
        return markerFactory
    }

    override fun getMarkerFactoryClassStr(): String {
        return BasicMarkerFactory::class.java.name
    }
}

/**
 * SLF4J MDC绑定
 * 使用RuntimeDependency注解简化SLF4J依赖管理
 */
@RuntimeDependency("org.slf4j:slf4j-api:1.7.36")
class StaticMDCBinder private constructor() {

    companion object {
        @JvmField
        val SINGLETON = StaticMDCBinder()

        @JvmStatic
        fun getSingleton(): StaticMDCBinder {
            return SINGLETON
        }
    }

    private val mdcAdapter = NOPMDCAdapter()

    fun getMDCA(): MDCAdapter {
        return mdcAdapter
    }

    fun getMDCAdapterClassStr(): String {
        return NOPMDCAdapter::class.java.name
    }
} 