package cc.trixey.invero.common.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import java.io.PrintStream

/**
 * SLF4J日志抑制器
 * 用于屏蔽SLF4J的初始化警告日志
 */
@PlatformSide(Platform.BUKKIT)
object SLF4JLoggerSuppressor {

    /**
     * 初始化时自动执行，比Invero主类的初始化更早
     */
    @Awake(LifeCycle.CONST)
    fun setupLoggerSuppressor() {
        // 保存原始的System.err输出流
        val originalErr = System.err
        
        try {
            // 设置一个临时的错误输出流，过滤掉SLF4J相关的初始化警告
            System.setErr(object : PrintStream(originalErr) {
                override fun println(message: String) {
                    if (!isSLF4JInitMessage(message)) {
                        super.println(message)
                    }
                }
                
                override fun print(message: String) {
                    if (!isSLF4JInitMessage(message)) {
                        super.print(message)
                    }
                }
            })
            
            // 触发SLF4J的初始化
            LoggerFactory.getLogger(SLF4JLoggerSuppressor::class.java)
            
            // 延迟一小段时间，确保SLF4J的初始化完成
            Thread.sleep(50)
            
        } catch (e: Exception) {
            // 出现错误时，打印错误信息
            originalErr.println("[Invero] 屏蔽SLF4J日志失败: ${e.message}")
        } finally {
            // 恢复原始的错误输出流
            System.setErr(originalErr)
        }
    }
    
    /**
     * 判断是否为SLF4J的初始化消息
     */
    private fun isSLF4JInitMessage(message: String): Boolean {
        return message.contains("SLF4J:") && (
                message.contains("No SLF4J providers were found") ||
                message.contains("Defaulting to no-operation (NOP) logger") ||
                message.contains("See https://www.slf4j.org/codes.html") ||
                message.contains("StaticLoggerBinder") ||
                message.contains("slf4j-api") ||
                message.contains("slf4j-simple") ||
                message.contains("SLF4J initialization")
        )
    }
} 