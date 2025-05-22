package utils

import org.slf4j.LoggerFactory

object Logger {
    private val logger = LoggerFactory.getLogger("KickstarterParser")
    fun info(msg: String) = logger.info(msg)
    fun warn(msg: String) = logger.warn(msg)
    fun error(msg: String) = logger.error(msg)
}
