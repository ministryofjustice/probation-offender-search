package uk.gov.justice.hmpps.offendersearch.util


import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.apache.commons.codec.binary.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.RSAPublicKeySpec
import java.time.Duration
import java.util.*


@Component
class JwtAuthenticationHelper(@Value("\${jwt.signing.key.pair}") privateKeyPair: String,
                              @Value("\${jwt.keystore.password}") keystorePassword: String,
                              @Value("\${jwt.keystore.alias}") keystoreAlias: String) {
  private val keyPair: KeyPair

  init {
    keyPair = getKeyPair(ByteArrayResource(Base64.decodeBase64(privateKeyPair)), keystoreAlias, keystorePassword.toCharArray())
  }

  fun createJwt(vararg roles: String): String {
    return createJwt(JwtParameters(
        username = "prison-to-probation",
        roles = listOf(*roles),
        scope = listOf("read", "write"),
        expiryTime = Duration.ofDays(1)))
  }

  fun createCommunityJwtWithScopes(vararg scopes: String): String {
    return createJwt(JwtParameters(
        username = "prison-to-probation",
        roles = listOf("ROLE_COMMUNITY"),
        scope = listOf(*scopes),
        expiryTime = Duration.ofDays(1)))
  }

  fun createJwt(parameters: JwtParameters): String {
    val claims = mapOf(
        "user_name" to parameters.username,
        "user_id" to parameters.userId,
        "client_id" to "offender-search",
        "authorities" to parameters.roles,
        "scope" to parameters.roles
    )
    return Jwts.builder()
        .setId(UUID.randomUUID().toString())
        .setSubject(parameters.username)
        .addClaims(claims)
        .setExpiration(Date(System.currentTimeMillis() + parameters.expiryTime.toMillis()))
        .signWith(SignatureAlgorithm.RS256, keyPair.private)
        .compact()
  }

  data class JwtParameters(
      val username: String,
      val userId: String? = null,
      val scope: List<String>,
      val roles: List<String>,
      val expiryTime: Duration
  )

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
