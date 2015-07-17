package carwings
package server

import unfiltered.filter.Planify
import unfiltered.jetty.Server.portBinding
import unfiltered.jetty.SocketPortBinding
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB

object Server {

  def main(args: Array[String]) {
    args match {
      case Array(port) =>
      val db = new DynamoDB(new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain()));
      val proxy = new api.Api {
        val vehicles = dynamodb.VehicleStoreDynamo(db)
      }

      portBinding(SocketPortBinding(port.toInt, "0.0.0.0"))
        .plan(Planify(proxy.intent))
        .run({
          server =>
          println(s"Server started on ${server}.")
        })
      case _ => println("Provide a port number")
    }
  }
}
