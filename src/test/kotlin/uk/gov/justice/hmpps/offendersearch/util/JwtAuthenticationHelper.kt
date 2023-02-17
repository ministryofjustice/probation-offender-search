package uk.gov.justice.hmpps.offendersearch.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthenticationHelper(private val keyPair: KeyPair) {
  fun createJwt(vararg roles: String): String = createJwt(
    subject = "new-tech",
    roles = listOf(*roles),
    scope = listOf("read", "write"),
  )

  fun createCommunityJwtWithScopes(vararg scopes: String): String = createJwt(
    subject = "new-tech",
    roles = listOf("ROLE_COMMUNITY"),
    scope = listOf(*scopes),
  )

  fun createCommunityJwtWithScopes(clientUser: ClientUser, vararg scopes: String): String = createJwt(
    subject = clientUser.subject,
    clientId = clientUser.clientId,
    username = clientUser.username,
    authSource = clientUser.authSource,
    roles = listOf("ROLE_COMMUNITY"),
    scope = listOf(*scopes),
  )

  fun createJwt(
    username: String? = null,
    subject: String? = null,
    clientId: String = "new-tech",
    authSource: String = "none",
    scope: List<String> = listOf(),
    roles: List<String> = listOf(),
    expiryTime: Duration = Duration.ofDays(1),
  ): String {

    val claims = mutableMapOf(
      "authorities" to roles,
      "scope" to scope,
      "client_id" to clientId,
      "auth_source" to authSource
    ).apply { username?.let { this["user_name"] = it } }

    return Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject(subject)
      .addClaims(claims)
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(keyPair.private, SignatureAlgorithm.RS256)
      .compact()
  }

  data class ClientUser(
    val clientId: String,
    val subject: String,
    val username: String? = null,
    val authSource: String = "delius"
  )
}
