package com.beancounter.marketdata.registration

import com.beancounter.auth.common.TokenService
import com.beancounter.common.contracts.RegistrationResponse
import com.beancounter.common.model.SystemUser
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import javax.transaction.Transactional

/**
 * Registration of authenticated users.
 */
@Service
@Transactional
class SystemUserService internal constructor(
    private val systemUserRepository: SystemUserRepository,
    private val tokenService: TokenService,
) {
    fun save(systemUser: SystemUser): SystemUser {
        return systemUserRepository.save(systemUser)
    }

    fun isServiceAccount(): Boolean {
        return tokenService.isServiceToken
    }

    fun find(id: String?): SystemUser? {
        if (id == null) return null
        return systemUserRepository.findById(id).orElse(null)
    }

    fun register(jwt: Jwt): RegistrationResponse {
        // ToDo: Find by email
        var result = find(jwt.subject)
        if (result == null) {
            val systemUser = SystemUser(jwt.subject, jwt.getClaim("email"))
            result = save(systemUser)
        }
        return RegistrationResponse(result)
    }

    fun getActiveUser(): SystemUser? {
        return find(tokenService.subject)
    }
}
