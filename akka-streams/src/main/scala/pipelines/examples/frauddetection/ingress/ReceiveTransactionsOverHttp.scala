package pipelines.examples.frauddetection.ingress

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import pipelines.akkastream.AkkaServerStreamlet
import pipelines.akkastream.util.scaladsl.HttpServerLogic
import pipelines.examples.frauddetection.data.CustomerTransaction
import pipelines.streamlets.StreamletShape
import pipelines.streamlets.avro.AvroOutlet

import pipelines.examples.frauddetection.utils.CustomerTransactionProtocol._

class ReceiveTransactionsOverHttp extends AkkaServerStreamlet {

  //\\//\\//\\ INLETS //\\//\\//\\

  //\\//\\//\\ OUTLETS //\\//\\//\\
  val out = AvroOutlet[CustomerTransaction]("transactions")

  //\\//\\//\\ SHAPE //\\//\\//\\
  final override val shape = StreamletShape.withOutlets(out)

  //\\//\\//\\ LOGIC //\\//\\//\\
  final override def createLogic =
    HttpServerLogic.default(this, out)
}

