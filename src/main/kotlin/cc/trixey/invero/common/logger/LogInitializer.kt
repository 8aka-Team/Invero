package cc.trixey.invero.common.logger

/**
 * 静态初始化类
 * 用于确保在类加载阶段就初始化日志拦截器
 */
class LogInitializer {
    
    companion object {
        /**
         * 静态初始化块，会在类加载时执行
         */
        init {
            try {
                // 初始化日志拦截器
                LoggerBlocker.init()
            } catch (e: Throwable) {
                // 确保初始化失败不会影响插件加载
                System.err.println("[Invero] 初始化日志拦截器失败: ${e.message}")
            }
        }
        
        /**
         * 静态实例，确保类被加载
         */
        @JvmField
        val INSTANCE = LogInitializer()
    }
    
    /**
     * 确保在类加载后立即初始化
     */
    init {
        LoggerBlocker.init()
    }
} 