package pipelines.examples.frauddetection.models.pmml

import com.lightbend.modelserving.model.pmml.PMMLModel
import com.lightbend.modelserving.model.{ Model, ModelDescriptor, ModelFactory }
import org.jpmml.evaluator.Computable
import pipelines.examples.frauddetection.data.CustomerTransaction

import scala.collection.JavaConverters._

/**
 * PMML model implementation for wine data.
 */
class FraudPMMLModel(descriptor: ModelDescriptor)
  extends PMMLModel[CustomerTransaction, Double](descriptor)(() ⇒ 0.0) {

  override protected def invokeModel(record: CustomerTransaction): Either[String, Double] = {
    // Clear arguments (from previous run)
    arguments.clear()
    // Populate input based on record
    inputFields.asScala.foreach(field ⇒
      arguments.put(
        field.getName,
        field.prepare(record.get(field.getName.getValue.replaceAll(" ", "_"))))
    )

    // Calculate Output
    try {
      val result = evaluator.evaluate(arguments.asJava)
      // Prepare output
      val d = result.get(tname) match {
        case c: Computable ⇒ c.getResult.toString.toDouble
        case v: Any        ⇒ v.asInstanceOf[Double]
      }
      Right(d)
    } catch {
      case t: Throwable ⇒
        t.printStackTrace()
        Left(t.getMessage)
    }
  }
}

/**
 * Factory for wine data PMML model
 */
object FraudPMMLModelFactory extends ModelFactory[CustomerTransaction, Double] {

  def make(descriptor: ModelDescriptor): Either[String, Model[CustomerTransaction, Double]] =
    Right(new FraudPMMLModel(descriptor))
}
