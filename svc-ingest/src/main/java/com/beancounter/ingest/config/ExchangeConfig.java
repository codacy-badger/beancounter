package com.beancounter.ingest.config;

import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Allow remapping of exchange related code data.
 *
 * @author mikeh
 * @since 2019-02-13
 */
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "beancounter.exchanges")
@Component
@Slf4j
public class ExchangeConfig {

  @Getter
  @Setter
  private Map<String, String> aliases;

  @PostConstruct
  public void logConfig() {
    log.info("{} exchanges loaded", aliases.size());
  }

  /**
   * Return the Exchange code to use for the supplied input.
   *
   * @param input code that *might* have an alias.
   * @return the alias or input if no exception is defined.
   */
  public String resolveAlias(String input) {
    String alias = aliases.get(input);
    if (alias == null) {
      return input;
    } else {
      return alias;
    }

  }
}