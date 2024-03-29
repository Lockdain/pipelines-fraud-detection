package pipelines.examples.frauddetection.egress

import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ Flow, GraphDSL, Merge, RunnableGraph, Sink }
import pipelines.akkastream.scaladsl.{ FlowWithOffsetContext, RunnableGraphStreamletLogic }
import pipelines.akkastream.{ AkkaStreamlet, StreamletLogic }
import pipelines.examples.frauddetection.data.{ CustomerTransaction, ScoredTransaction }
import pipelines.streamlets.StreamletShape
import pipelines.streamlets.avro.AvroInlet

/**
 * Logs all Transactions at the end of our stream
 */
class LogCustomerTransactions extends AkkaStreamlet {

  //\\//\\//\\ INLETS //\\//\\//\\
  val fromTheModel = AvroInlet[ScoredTransaction]("model")
  val fromTheMerchant = AvroInlet[CustomerTransaction]("merchant")

  //\\//\\//\\ OUTLETS //\\//\\//\\

  //\\//\\//\\ SHAPE //\\//\\//\\
  final override val shape = StreamletShape.withInlets(fromTheModel, fromTheMerchant)

  //\\//\\//\\ LOGIC //\\//\\//\\
  final override def createLogic(): StreamletLogic = new RunnableGraphStreamletLogic() {

    val theModelFlow = Flow[ScoredTransaction].map { stx ⇒
      //      influxDb.writeEnd(stx.inputRecord.transactionId, stx. .modelResultMetadata.modelName, stx.modelResult.value)

      if (stx.modelResult.value > 0.7) {
        system.log.info(s"${stx.inputRecord.transactionId} ********************* FRAUD *********************")
      } else {
        system.log.info(s"${stx.inputRecord.transactionId} Ok")
      }

      stx.inputRecord
    }

    val theMerchantFlow = Flow[CustomerTransaction].map { tx ⇒
      system.log.info(s"${tx.transactionId} Ok")
      tx
    }

    def runnableGraph() =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] ⇒
        import GraphDSL.Implicits._

        val model = plainSource(fromTheModel).via(theModelFlow)
        val merchant = plainSource(fromTheMerchant).via(theMerchantFlow)
        val out = Sink.ignore

        val merge = builder.add(Merge[(CustomerTransaction)](2))

        model ~> merge ~> out
        merchant ~> merge

        ClosedShape
      })
  }
}
