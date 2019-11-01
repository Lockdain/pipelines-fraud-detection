package pipelines.examples.frauddetection.ingress

import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.modelserving.model.{ ModelDescriptor, ModelType }
import pipelines.akkastream.AkkaStreamlet
import pipelines.akkastream.scaladsl.RunnableGraphStreamletLogic
import pipelines.examples.frauddetection.utils
import pipelines.streamlets.StreamletShape
import pipelines.streamlets.avro.AvroOutlet
import pipelinesx.config.ConfigUtil
import pipelinesx.config.ConfigUtil.implicits._
import pipelinesx.logging.{ Logger, LoggingUtil }

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
 * Loads a model and sends it downstream
 */
final case object GenerateFraudDetectionModels extends AkkaStreamlet {

  //\\//\\//\\ INLETS //\\//\\//\\

  //\\//\\//\\ OUTLETS //\\//\\//\\
  val modelsGoOutHere = AvroOutlet[ModelDescriptor]("models", _.modelType.toString)

  //\\//\\//\\ SHAPE //\\//\\//\\
  final override val shape = StreamletShape(modelsGoOutHere)

  //\\//\\//\\ LOGIC //\\//\\//\\
  final override def createLogic =
    new RunnableGraphStreamletLogic() {
      def runnableGraph =
        GenerateFraudDetectionModelsUtil.makeSource().to(plainSink(modelsGoOutHere))
    }
}

object GenerateFraudDetectionModelsUtil {

  val logger: Logger = LoggingUtil.getLogger(GenerateFraudDetectionModelsUtil.getClass)

  lazy val modelFrequencySeconds: FiniteDuration =
    ConfigUtil.default
      .getOrElse[Int]("fraud-detection.model-frequency-seconds")(5).seconds

  lazy val fraudModelsResources: Map[ModelType, Seq[String]] =
    ConfigUtil.defaultConfig
      .getObject("fraud-detection.model-sources").entrySet.asScala.foldLeft(
        Map.empty[ModelType, Seq[String]]) {
          case (map, e) ⇒
            val modelType = ModelType.valueOf(e.getKey.toUpperCase)
            val list = e.getValue.valueType.toString match {
              case "LIST"   ⇒ e.getValue.unwrapped.asInstanceOf[java.util.ArrayList[String]].toArray.map(_.toString)
              case "STRING" ⇒ Array(e.getValue.unwrapped.toString)
            }
            map + (modelType -> list)
        }

  def makeSource(
      modelsResources: Map[ModelType, Seq[String]] = fraudModelsResources,
      frequency:       FiniteDuration              = modelFrequencySeconds): Source[ModelDescriptor, NotUsed] = {
    val recordsReader = utils.FraudModelReader(modelsResources)
    Source.repeat(recordsReader)
      .map(reader ⇒ {
        val newModel = reader.next()
        logger.info(s"New Model Deployed: ${newModel.modelName}")
        ModelDescriptor(newModel.modelName + "-" + UUID.randomUUID().toString, newModel.description, newModel.modelType,
          newModel.modelBytes, newModel.modelSourceLocation)
      })
      .throttle(1, frequency)
  }
}
