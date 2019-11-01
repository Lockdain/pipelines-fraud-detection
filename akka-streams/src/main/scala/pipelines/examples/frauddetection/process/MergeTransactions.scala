package pipelines.examples.frauddetection.process

import pipelines.akkastream.util.scaladsl.MergeLogic
import pipelines.akkastream.{ AkkaStreamlet, StreamletLogic }
import pipelines.examples.frauddetection.data.CustomerTransaction
import pipelines.streamlets.StreamletShape
import pipelines.streamlets.avro.{ AvroInlet, AvroOutlet }

class MergeTransactions extends AkkaStreamlet {

  //\\//\\//\\ INLETS //\\//\\//\\
  val fromTheLeft = AvroInlet[CustomerTransaction]("left")
  val fromTheRight = AvroInlet[CustomerTransaction]("right")

  //\\//\\//\\ OUTLETS //\\//\\//\\
  val everythingGoesOutHere = AvroOutlet[CustomerTransaction]("transactions")

  //\\//\\//\\ SHAPE //\\//\\//\\
  final override val shape = StreamletShape.withInlets(fromTheLeft, fromTheRight).withOutlets(everythingGoesOutHere)

  //\\//\\//\\ LOGIC //\\//\\//\\
  final override def createLogic(): StreamletLogic =
    new MergeLogic[CustomerTransaction](Vector(fromTheLeft, fromTheRight), everythingGoesOutHere)
}
