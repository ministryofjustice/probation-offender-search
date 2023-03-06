package uk.gov.justice.hmpps.probationsearch.services

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.hmpps.probationsearch.security.AuthAwareTokenConverter
import uk.gov.justice.hmpps.probationsearch.wiremock.CommunityApiExtension
import java.security.KeyPair
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID

@ExtendWith(CommunityApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
@DirtiesContext(classMode = BEFORE_CLASS)
internal class CommunityServiceTest {
  @Autowired
  private lateinit var service: CommunityService

  @Autowired
  private lateinit var keyPair: KeyPair

  private lateinit var jwt: Jwt

  @BeforeEach
  internal fun setUp() {
    jwt = createJwt()
    CommunityApiExtension.communityApi.resetMappings()
    SecurityContextHolder.setContext(SecurityContextImpl(AuthAwareTokenConverter().convert(jwt)))
  }

  @Nested
  inner class CanAccessOffender {
    @Test
    fun `will get access limitation using the crn `() {

      CommunityApiExtension.communityApi.stubUserAccess(
        "X12345",
        """
        {
            "userRestricted": false,
            "userExcluded": false
        }
        """.trimIndent()
      )

      val accessLimitation = service.canAccessOffender("X12345")

      CommunityApiExtension.communityApi.verify(
        getRequestedFor(urlEqualTo("/secure/offenders/crn/X12345/userAccess"))
          .withHeader("Authorization", WireMock.equalTo("Bearer ${jwt.tokenValue}"))
      )

      assertThat(accessLimitation.userExcluded).isFalse
      assertThat(accessLimitation.userRestricted).isFalse
    }

    @Test
    fun `will handle access denied`() {

      CommunityApiExtension.communityApi.stubUserAccessDenied(
        "X12345",
        """
        {
            "userRestricted": false,
            "userExcluded": true
        }
        """.trimIndent()
      )

      val accessLimitation = service.canAccessOffender("X12345")

      CommunityApiExtension.communityApi.verify(
        getRequestedFor(urlEqualTo("/secure/offenders/crn/X12345/userAccess"))
          .withHeader("Authorization", WireMock.equalTo("Bearer ${jwt.tokenValue}"))
      )

      assertThat(accessLimitation.userExcluded).isTrue
      assertThat(accessLimitation.userRestricted).isFalse
    }

    @Test
    fun `a 404 not found is treated as no access`() {
      CommunityApiExtension.communityApi.stubUserAccessNotFound("X12345")

      val accessLimitation = service.canAccessOffender("X12345")

      assertThat(accessLimitation.userExcluded).isTrue
      assertThat(accessLimitation.userRestricted).isTrue
    }

    @Test
    fun `other errors are treated as exceptions`() {
      CommunityApiExtension.communityApi.stubUserAccessError("X12345")

      assertThatThrownBy { service.canAccessOffender("X12345") }.isInstanceOf(WebClientResponseException.InternalServerError::class.java)
    }
  }

  private fun createJwt(): Jwt {
    val claims = mapOf(
      "authorities" to listOf("ROLE_COMMUNITY"),
      "scope" to listOf("read"),
      "client_id" to "new-tech",
      "auth_source" to "delius",
      "user_name" to "maryblack",
      "sub" to "maryblack",
    )
    val expiresAt = LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)
    val token = Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .addClaims(claims)
      .setExpiration(Date(expiresAt.toEpochMilli()))
      .signWith(keyPair.private, SignatureAlgorithm.RS256)
      .compact()

    return Jwt(
      token,
      LocalDateTime.now().toInstant(ZoneOffset.UTC),
      expiresAt,
      mapOf("a" to "b"),
      claims,
    )
  }
}
