package cc.trixey.invero.common.logger

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide

/**
 * TabooLib 早期初始化类
 * 确保在TabooLib加载阶段就拦截SLF4J日志
 */
@PlatformSide(Platform.BUKKIT)
object EarlyInitializer {
    
    /**
     * 静态字段，确保LogInitializer被加载
     */
    private val initializer = LogInitializer.INSTANCE
    
    /**
     * 在TabooLib常量阶段执行，这是最早的初始化阶段
     */
    @Awake(LifeCycle.CONST)
    fun init() {
        // 确保LoggerBlocker被初始化
        LoggerBlocker.init()
        
        // 触发静态初始化
        LogInitializer.INSTANCE
    }
    
    /**
     * 在TabooLib加载阶段执行
     */
    @Awake(LifeCycle.LOAD)
    fun load() {
        LoggerBlocker.init()
    }
    
    /**
     * 在TabooLib启用阶段执行
     */
    @Awake(LifeCycle.ENABLE)
    fun enable() {
        LoggerBlocker.init()
    }
    
    /**
     * 在TabooLib停用阶段执行
     */
    @Awake(LifeCycle.DISABLE)
    fun disable() {
        LoggerBlocker.restore()
    }
} 