package com.beancounter.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Classification for integration or other system failures.
 *
 * @author mikeh
 * @since 2019-02-03
 */
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class SystemException extends RuntimeException {

  public SystemException(String reason) {
    super(reason);
  }

}