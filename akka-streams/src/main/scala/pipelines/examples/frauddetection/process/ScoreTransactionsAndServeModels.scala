package pipelines.examples.frauddetection.process

import java.io.File

import akka.Done
import akka.pattern.ask
import akka.stream.scaladsl.{ Flow, Sink }
import akka.util.Timeout
import com.lightbend.modelserving.model.actor.ModelServingActor
import com.lightbend.modelserving.model.persistence.ModelPersistence
import com.lightbend.modelserving.model.{ Model, ModelDescriptor, ModelType, MultiModelFactory }
import pipelines.akkastream.AkkaStreamlet
import pipelines.akkastream.scaladsl.RunnableGraphStreamletLogic
import pipelines.examples.frauddetection.data.{ CustomerTransaction, ScoreFromTheModel, ScoredTransaction }
import pipelines.examples.frauddetection.models.h20.FraudH2OModelFactory
import pipelines.examples.frauddetection.models.pmml.FraudPMMLModelFactory
import pipelines.examples.frauddetection.models.tensorflow.{ FraudTensorFlowBundledModelFactory, FraudTensorFlowModelFactory }
import pipelines.streamlets.StreamletShape
import pipelines.streamlets.avro.{ AvroInlet, AvroOutlet }

import scala.concurrent.duration._

final case object ScoreTransactionsAndServeModels extends AkkaStreamlet {

  //\\//\\//\\ INLETS //\\//\\//\\
  val transactionsComeInHere = AvroInlet[CustomerTransaction]("transactions")
  val modelsComeInHere = AvroInlet[ModelDescriptor]("models")

  //\\//\\//\\ OUTLETS //\\//\\//\\
  val scoredTransactionsComeOutHere = AvroOutlet[ScoredTransaction]("results")

  //\\//\\//\\ SHAPE //\\//\\//\\
  final override val shape = StreamletShape.withInlets(transactionsComeInHere, modelsComeInHere).withOutlets(scoredTransactionsComeOutHere)

  val modelFactory = MultiModelFactory(
    Map(
      ModelType.PMML -> FraudPMMLModelFactory,
      ModelType.TENSORFLOW -> FraudTensorFlowModelFactory,
      ModelType.TENSORFLOWSAVED -> FraudTensorFlowBundledModelFactory,
      ModelType.H2O -> FraudH2OModelFactory))

  //\\//\\//\\ LOGIC //\\//\\//\\
  final override def createLogic = new RunnableGraphStreamletLogic() {

    def runnableGraph() = {
      plainSource(modelsComeInHere).via(theModelFlow).runWith(Sink.ignore)
      plainSource(transactionsComeInHere).via(theTransactionsFlow).to(plainSink(scoredTransactionsComeOutHere))
    }

    implicit val askTimeout: Timeout = Timeout(30.seconds)

    val modelPersist = ModelPersistence[CustomerTransaction, Double](
      modelName = context.streamletRef,
      modelFactory = modelFactory,
      baseDirPath = new File("./"))

    val modelserver = context.system.actorOf(
      ModelServingActor.props[CustomerTransaction, Double](
        "fraud",
        modelFactory,
        modelPersist,
        () ⇒ 0.0))

    protected def theTransactionsFlow =
      Flow[CustomerTransaction].mapAsync(1) { transaction ⇒
        modelserver.ask(transaction).mapTo[Model.ModelReturn[Double]]
          .map { modelReturn ⇒
            val result = ScoreFromTheModel(value = modelReturn.modelOutput)
            ScoredTransaction(transaction, result)
          }
      }

    protected def theModelFlow =
      Flow[ModelDescriptor].mapAsync(1) {
        descriptor ⇒ modelserver.ask(descriptor).mapTo[Done]
      }
  }
}
