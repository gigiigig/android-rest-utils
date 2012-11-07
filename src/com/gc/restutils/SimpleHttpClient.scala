package com.gc.restutils

import java.net.URI
import java.net.URLEncoder
import java.util.ArrayList
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.conn.params.ConnRoutePNames

/**
 * Simple HTTP client based on [[http://hc.apache.org/ Apache HttpComponents]].
 *
 * This is a simplest example of HTTP GET:
 * {{{
 * using(new SimpleHttpClient()){ client =>
 *   val content: String = client.get(url = "http://localhost:8080/",
 *     responseHandler = { r => new String(EntityUtils.toByteArray(r.getEntity()), "UTF-8") }
 *   )
 * }
 * }}}
 *
 * You can give request parameters and the encoding through the constructor.
 * {{{
 * using(new SimpleHttpClient()){ client =>
 *   val content: String = client.get(
 *     url    = "http://localhost:8080/",
 *     params = Map("lang" -> "Scala"),
 *     encode = "UTF-8",
 *     responseHandler = { r =>
 *       new String(EntityUtils.toByteArray(r.getEntity()), "UTF-8")
 *     }
 *   )
 * }
 * }}}
 *
 * @param url the request URL
 * @param params request parameters as a Map. if it's not necessary, you can omit this.
 * @param encode the charactor encoding of request parameters (optional).
 *               the default value is "UTF-8"
 */
class SimpleHttpClient(proxy: String = null) {

  val client = new DefaultHttpClient

  if(proxy != null){
    val Array(host, port) =  proxy.split(":")
    client.getParams().setParameter(
        ConnRoutePNames.DEFAULT_PROXY, new HttpHost(host, port.toInt, "http"))
  }

  /**
   * Sends the GET request and invokes the response handler given as the argument.
   *
   * @param responseHandler the response handler. if you omit this, the default handler is used.
   *   the default handler returns the response body as Option[String] if the response status is 200,
   *   otherwise returns None.
   */
  def get[T](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(),
      encode: String = "UTF-8", responseHandler: (HttpResponse  => T) = defaultResponseHandler _): T = {

    // assemble query string
    val queryString = params.map(e =>
      e._1 + "=" + URLEncoder.encode(e._2, encode)
    ).mkString("", "&", "")

    val get = new HttpGet(new URI(if(queryString.isEmpty) url else url + "?" + queryString))

    // set headers
    headers.foreach { e => get.addHeader(e._1, e._2) }

    responseHandler(client.execute(get))
  }

  /**
   * Sends the POST request and invokes the response handler given as the argument.
   *
   * @param responseHandler the response handler. if you omit this, the default handler is used.
   *   the default handler returns the response body as Option[String] if the response status is 200,
   *   otherwise returns None.
   */
  def post[T](url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map(),
      encode: String = "UTF-8", responseHandler: (HttpResponse => T) = defaultResponseHandler _): T= {

    // assemble form params
    val formParams = new ArrayList[NameValuePair]();
    params.foreach { e => formParams.add(new BasicNameValuePair(e._1, e._2)) }

    val post = new HttpPost(new URI(url))

    // set headers
    headers.foreach { e => post.addHeader(e._1, e._2) }

    // set form params as request body
    post.setEntity(new UrlEncodedFormEntity(formParams, encode))

    responseHandler(client.execute(post))
  }

  def close(): Unit = {
    client.getConnectionManager.shutdown
  }

  /**
   * The default response handler.
   *
   * If you haven't given the response handler to get() or post(),
   * SimpleHttpClient uses this method to handle response.
   *
   * @return if the response status is 200 then this handler returns
   *         the response body as a string, otherwise None.
   */
  private def defaultResponseHandler(r: HttpResponse): Option[String] = {
    val contentType = r.getHeaders("content-type")
    val pattern = "^.+;charset=(.+?)$".r

    // TODO Should it check the encoding is valid...?
    val encode = contentType(0).getValue() match {
      case pattern(e) => e
      case _ => "UTF-8"
    }

    r.getStatusLine.getStatusCode match {
      case HttpStatus.SC_OK => Some(new String(EntityUtils.toByteArray(r.getEntity), encode))
      case status => None
    }
  }

}