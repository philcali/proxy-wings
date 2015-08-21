package carwings
package server

import unfiltered.filter.Planify
import unfiltered.jetty.Server.portBinding
import unfiltered.jetty.SocketPortBinding
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB

object Server {
  def startServer(port: Int, endpoint: Option[String]) {
    val aws = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain())
    endpoint.foreach(aws.setEndpoint)
    val db = new DynamoDB(aws)
    val proxy = new api.Api {
      val vehicles = dynamodb.VehicleStoreDynamo(db)
    }
    portBinding(SocketPortBinding(port, "0.0.0.0"))
      .plan(Planify(proxy.intent))
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
