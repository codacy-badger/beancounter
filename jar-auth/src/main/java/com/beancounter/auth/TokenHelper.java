package com.beancounter.auth;

import com.beancounter.common.model.SystemUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * TestHelper class that generates JWT tokens you can test with.
 */
@UtilityClass
public class TokenHelper {

  public static final String SCOPE = "beancounter profile email";

  public Jwt getUserToken(SystemUser systemUser) {
    return getUserToken(systemUser, getDefaultRoles());
  }

  public Jwt getUserToken(SystemUser systemUser, Map<String, Collection<String>> realmAccess) {

    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(systemUser.getId())
        .claim("email", systemUser.getEmail())
        .claim("realm_access", realmAccess)
        .claim("scope", SCOPE)
        .expiresAt(new Date(System.currentTimeMillis() + 60000).toInstant())
        .build();
  }

  public Map<String, Collection<String>> getDefaultRoles() {
    return getRoles("user");
  }

  public Map<String, Collection<String>> getRoles(String... roles) {
    Collection<String> userRoles = new ArrayList<>();
    Collections.addAll(userRoles, roles);

    Map<String, Collection<String>> realmAccess = new HashMap<>();
    realmAccess.put("roles", userRoles);
    return realmAccess;

  }
}
