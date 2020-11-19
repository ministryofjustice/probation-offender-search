/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package uk.gov.justice.hmpps.offendersearch.config

import com.amazonaws.DefaultRequest
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.Signer
import com.amazonaws.http.HttpMethodName
import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpCoreContext
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.TreeMap

/**
 * An [HttpRequestInterceptor] that signs requests using any AWS [Signer]
 * and [AWSCredentialsProvider].
 */
class AWSRequestSigningApacheInterceptor(
  /**
   * The service that we're connecting to. Technically not necessary.
   * Could be used by a future Signer, though.
   */
  private val service: String,
  /**
   * The particular signer implementation.
   */
  private val signer: Signer,
  /**
   * The source of AWS credentials for signing.
   */
  private val awsCredentialsProvider: AWSCredentialsProvider
) : HttpRequestInterceptor {

  /**
   * {@inheritDoc}
   */
  @Throws(HttpException::class, IOException::class)
  override fun process(request: HttpRequest, context: HttpContext) {
    val uriBuilder: URIBuilder
    uriBuilder = try {
      URIBuilder(request.requestLine.uri)
    } catch (e: URISyntaxException) {
      throw IOException("Invalid URI", e)
    }
    // Copy Apache HttpRequest to AWS DefaultRequest
    val signableRequest: DefaultRequest<*> = DefaultRequest<Any>(service)
    val host = context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST) as HttpHost
    signableRequest.endpoint = URI.create(host.toURI())
    val httpMethod = HttpMethodName.fromValue(request.requestLine.method)
    signableRequest.httpMethod = httpMethod
    try {
      signableRequest.resourcePath = uriBuilder.build().rawPath
    } catch (e: URISyntaxException) {
      throw IOException("Invalid URI", e)
    }
    if (request is HttpEntityEnclosingRequest) {
      val httpEntityEnclosingRequest = request
      if (httpEntityEnclosingRequest.entity != null) {
        signableRequest.content = httpEntityEnclosingRequest.entity.content
      }
    }
    signableRequest.parameters = nvpToMapParams(uriBuilder.queryParams)
    signableRequest.headers = headerArrayToMap(request.allHeaders)
    // Sign it
    signer.sign(signableRequest, awsCredentialsProvider.credentials)
    // Now copy everything back
    request.setHeaders(mapToHeaderArray(signableRequest.headers))
    if (request is HttpEntityEnclosingRequest) {
      val httpEntityEnclosingRequest = request
      if (httpEntityEnclosingRequest.entity != null) {
        val basicHttpEntity = BasicHttpEntity()
        basicHttpEntity.content = signableRequest.content
        httpEntityEnclosingRequest.entity = basicHttpEntity
      }
    }
  }

  companion object {
    /**
     *
     * @param params list of HTTP query params as NameValuePairs
     * @return a multimap of HTTP query params
     */
    private fun nvpToMapParams(params: List<NameValuePair>): Map<String, MutableList<String>> {
      val parameterMap: MutableMap<String, MutableList<String>> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)
      for (nvp in params) {
        val argsList = parameterMap.computeIfAbsent(nvp.name) { _: String? -> ArrayList() }
        argsList.add(nvp.value)
      }
      return parameterMap
    }

    /**
     * @param headers modeled Header objects
     * @return a Map of header entries
     */
    private fun headerArrayToMap(headers: Array<Header>): Map<String, String> {
      val headersMap: MutableMap<String, String> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)
      for (header in headers) {
        if (!skipHeader(header)) {
          headersMap[header.name] = header.value
        }
      }
      return headersMap
    }

    /**
     * @param header header line to check
     * @return true if the given header should be excluded when signing
     */
    private fun skipHeader(header: Header): Boolean {
      return (
        (
          "content-length".equals(header.name, ignoreCase = true) &&
            "0" == header.value
          ) || // Strip Content-Length: 0
          "host".equals(header.name, ignoreCase = true) // Host comes from endpoint
        )
    }

    /**
     * @param mapHeaders Map of header entries
     * @return modeled Header objects
     */
    private fun mapToHeaderArray(mapHeaders: Map<String, String>): Array<Header?> {
      val headers = arrayOfNulls<Header>(mapHeaders.size)
      var i = 0
      for ((key, value) in mapHeaders) {
        headers[i++] = BasicHeader(key, value)
      }
      return headers
    }
  }
}
