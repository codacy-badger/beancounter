package com.beancounter.position.integration;

import com.beancounter.position.PositionBoot;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PositionBoot.class)
@WebAppConfiguration
@ActiveProfiles("test")
@Tag("slow")
class PositionBootTests {
  @Autowired
  private WebApplicationContext context;

  @Test
  void contextLoads() {
  }

}
