package carwings
package server

import unfiltered.filter.Planify
import unfiltered.jetty.PropertySslContextProvider
import unfiltered.jetty.Server.{ http, portBinding, defaultKeystorePathProperty, defaultKeystorePasswordProperty }
import unfiltered.jetty.SslSocketPortBinding
import org.eclipse.jetty.util.ssl.SslContextFactory
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB

object Server {
  class SafePropertySslContextProvider extends PropertySslContextProvider(
    defaultKeystorePathProperty,
    defaultKeystorePasswordProperty
  ) {
    override lazy val sslContextFactory = {
      val factory = new SslContextFactory
      factory.setKeyStorePath(keyStorePath)
      factory.setKeyStorePassword(keyStorePassword)
      factory.setExcludeCipherSuites(
        "SSL_RSA_WITH_DES_CBC_SHA",
        "SSL_DHE_RSA_WITH_DES_CBC_SHA",
        "SSL_DHE_DSS_WITH_DES_CBC_SHA",
        "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
        "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA")
      factory.setExcludeProtocols(
        "SSL",
        "SSLv2",
        "SSLv2Hello",
        "SSLv3")
      factory
    }
  }

  def startServer(port: Int, endpoint: Option[String]) {
    val aws = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain())
    endpoint.foreach(aws.setEndpoint)
    val db = new DynamoDB(aws)
    val proxy = new api.Api {
      val vehicles = dynamodb.VehicleStoreDynamo(db)
    }
    (if (port == 443)
      portBinding(SslSocketPortBinding(port, "0.0.0.0", new SafePropertySslContextProvider)) else
      http(port, "0.0.0.0")
    ).plan(Planify(proxy.intent))
      .resources(getClass.getClassLoader.getResource("public"))
      .run({
        server =>
        println(s"Server started on ${server}.")
      })
  }

  def main(args: Array[String]) {
    args match {
      case Array(port, endppoint) =>
      startServer(port.toInt, Some(endppoint))
      case Array(port) =>
      startServer(port.toInt, None)
      case _ => println("Provide a port number")
    }
  }
}
