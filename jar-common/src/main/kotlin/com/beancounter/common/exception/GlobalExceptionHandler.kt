package com.beancounter.common.exception

import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.ResourceAccessException
import java.net.ConnectException
import javax.servlet.http.HttpServletRequest

/**
 * When an exception is thrown, it is intercepted by this class and a JSON friendly response is returned.
 */
@ControllerAdvice
class GlobalExceptionHandler {
    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(ConnectException::class, ResourceAccessException::class, FeignException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleSystemException(request: HttpServletRequest, e: Throwable): SpringExceptionMessage =
        SpringExceptionMessage(
            error = "Unable to contact dependent system.",
            message = e.message,
            path = request.requestURI
        ).also { log.error(e.message) }

    private val errorMessage = "We are unable to process your request."

    @ExceptionHandler(BusinessException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleBusinessException(request: HttpServletRequest, e: BusinessException) = SpringExceptionMessage(
        error = errorMessage,
        message = e.message,
        path = request.requestURI
    )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleBadRequest(request: HttpServletRequest) = SpringExceptionMessage(
        error = errorMessage,
        message = "Message not readable",
        path = request.requestURI
    )

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun handleIntegrity(request: HttpServletRequest, e: Throwable) = SpringExceptionMessage(
        error = "Not processed",
        message = "Data integrity violation",
        path = request.requestURI
    )
}
