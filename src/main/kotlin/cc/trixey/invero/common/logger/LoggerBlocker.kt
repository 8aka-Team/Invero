package cc.trixey.invero.common.logger

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 错误日志屏蔽器
 * 用于屏蔽SLF4J的初始化警告日志
 */
object LoggerBlocker {
    
    private val originalErrStream = System.err
    private val filteredStream = FilteredPrintStream(originalErrStream)
    private var installed = false
    
    /**
     * 在最早的初始化阶段执行
     * 拦截系统错误输出
     */
    @Awake(LifeCycle.CONST)
    fun init() {
        if (!installed) {
            System.setErr(filteredStream)
            installed = true
        }
    }
    
    /**
     * 在插件关闭时恢复原始的错误输出流
     */
    @Awake(LifeCycle.DISABLE)
    fun restore() {
        if (installed) {
            System.setErr(originalErrStream)
            installed = false
        }
    }
    
    /**
     * 过滤的PrintStream实现
     */
    private class FilteredPrintStream(private val original: PrintStream) : PrintStream(original) {
        
        // 缓存错误消息
        private val recentMessages = ConcurrentLinkedQueue<String>()
        private val maxRecentMessages = 100
        
        // 捕获行
        private val lineBuffer = ByteArrayOutputStream()
        
        override fun print(s: String) {
            if (!isSLF4JMessage(s)) {
                original.print(s)
            }
        }
        
        override fun println(s: String) {
            if (!isSLF4JMessage(s)) {
                original.println(s)
            }
        }
        
        override fun write(buf: ByteArray, off: Int, len: Int) {
            // 收集缓冲区
            lineBuffer.write(buf, off, len)
            
            // 检查换行符
            val lineBytes = lineBuffer.toByteArray()
            val line = String(lineBytes)
            
            if (line.contains('\n') || line.contains('\r')) {
                
                val lines = line.split("[\r\n]+".toRegex())
                lineBuffer.reset()
                
                if (line.endsWith('\r') || line.endsWith('\n')) {
                } else {
                    val lastLine = lines.last()
                    if (lastLine.isNotEmpty()) {
                        lineBuffer.write(lastLine.toByteArray())
                    }
                }
                
                // 处理完整的行
                for (i in 0 until lines.size - 1) {
                    val completeLine = lines[i]
                    if (completeLine.isNotEmpty()) {
                        if (!isSLF4JMessage(completeLine)) {
                            original.println(completeLine)
                        }
                        
                        // 保存到最近消息队列
                        addRecentMessage(completeLine)
                    }
                }
            }
        }
        
        override fun write(b: Int) {
            lineBuffer.write(b)
            val line = String(lineBuffer.toByteArray())
            
            if (b == '\n'.code || b == '\r'.code) {
                lineBuffer.reset()
                if (!isSLF4JMessage(line.trim())) {
                    original.print(line)
                }
                
                // 保存到最近消息队列
                addRecentMessage(line.trim())
            }
        }
        
        /**
         * 添加消息到最近的消息队列
         */
        private fun addRecentMessage(message: String) {
            recentMessages.add(message)
            while (recentMessages.size > maxRecentMessages) {
                recentMessages.poll()
            }
        }
        
        /**
         * 判断SLF4J
         */
        private fun isSLF4JMessage(message: String): Boolean {
            return (message.contains("SLF4J:") && (
                    message.contains("No SLF4J providers were found") ||
                    message.contains("Defaulting to no-operation") ||
                    message.contains("See https://www.slf4j.org/codes.html") ||
                    message.contains("StaticLoggerBinder") ||
                    message.contains("Failed to load class") ||
                    message.contains("slf4j-api") ||
                    message.contains("slf4j-simple")
            )) || message.trim().startsWith("[Invero] SLF4J:")
        }
    }
}