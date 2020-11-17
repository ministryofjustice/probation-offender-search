package uk.gov.justice.hmpps.offendersearch.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.RSAPublicKeySpec
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthenticationHelper(private val keyPair: KeyPair) {
  fun createJwt(vararg roles: String): String {
    return createJwt(
      JwtParameters(
        subject = "new-tech",
        clientId = "new-tech",
        roles = listOf(*roles),
        scope = listOf("read", "write"),
        expiryTime = Duration.ofDays(1)
      )
    )
  }

  fun createCommunityJwtWithScopes(vararg scopes: String): String {
    return createJwt(
      JwtParameters(
        subject = "new-tech",
        clientId = "new-tech",
        roles = listOf("ROLE_COMMUNITY"),
        scope = listOf(*scopes),
        expiryTime = Duration.ofDays(1)
      )
    )
  }

  fun createCommunityJwtWithScopes(clientUser: ClientUser, vararg scopes: String): String {
    return createJwt(
      JwtParameters(
        subject = clientUser.subject,
        clientId = clientUser.clientId,
        username = clientUser.username,
        authSource = clientUser.authSource,
        roles = listOf("ROLE_COMMUNITY"),
        scope = listOf(*scopes),
        expiryTime = Duration.ofDays(1)
      )
    )
  }

  fun createJwt(parameters: JwtParameters): String {
    val claims = mapOf(
      "authorities" to parameters.roles,
      "scope" to parameters.scope,
      "client_id" to parameters.clientId,
      "auth_source" to parameters.authSource
    ).apply {
      parameters.username?.run { this + ("user_name" to parameters.username) }
    }

    return Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject(parameters.subject)
      .addClaims(claims)
      .setExpiration(Date(System.currentTimeMillis() + parameters.expiryTime.toMillis()))
      .signWith(SignatureAlgorithm.RS256, keyPair.private)
      .compact()
  }

  data class JwtParameters(
    val username: String? = null,
    val subject: String? = null,
    val clientId: String = "new-tech",
    val authSource: String = "none",
    val scope: List<String>,
    val roles: List<String>,
    val expiryTime: Duration
  )

  data class ClientUser(val clientId: String, val subject: String, val username: String?, val authSource: String = "delius")

  private fun getKeyPair(resource: Resource, alias: String, password: CharArray): KeyPair {
    val store = KeyStore.getInstance("jks")

    resource.inputStream.use {
      store.load(it, password)
      val key = store.getKey(alias, password) as RSAPrivateCrtKey
      val spec = RSAPublicKeySpec(key.modulus, key.publicExponent)
      val publicKey = KeyFactory.getInstance("RSA").generatePublic(spec)
      return KeyPair(publicKey, key)
    }
  }
}
