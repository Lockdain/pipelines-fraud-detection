package pipelines.examples.frauddetection.models.h20

import com.lightbend.modelserving.model.{ Model, ModelDescriptor, ModelFactory }
import com.lightbend.modelserving.model.h2o.H2OModel
import hex.genmodel.easy.RowData
import pipelines.examples.frauddetection.data.CustomerTransaction

class FraudH20Model(descriptor: ModelDescriptor)
  extends H2OModel[CustomerTransaction, Double](descriptor)(() ⇒ 0.0) {
  println(s"Creating FraudH2OModel ${descriptor.description}")

  /** Convert input record to raw data for serving. */
  protected def toRow(record: CustomerTransaction): RowData = {

    val row = new RowData
    row.put("Time", record.time.toString)
    row.put("V1", record.v1.toString)
    row.put("V2", record.v2.toString)
    row.put("V3", record.v3.toString)
    row.put("V4", record.v4.toString)
    row.put("V5", record.v5.toString)
    row.put("V6", record.v6.toString)
    row.put("V7", record.v7.toString)
    row.put("V9", record.v9.toString)
    row.put("V10", record.v10.toString)
    row.put("V11", record.v11.toString)
    row.put("V12", record.v12.toString)
    row.put("V14", record.v14.toString)
    row.put("V16", record.v16.toString)
    row.put("V17", record.v17.toString)
    row.put("V18", record.v18.toString)
    row.put("V19", record.v19.toString)
    row.put("V20", 0.toString)
    row.put("V21", record.v21.toString)
    row.put("V22", 0.toString)
    row.put("V23", 0.toString)
    row.put("V24", 0.toString)
    row.put("V25", 0.toString)
    row.put("V26", 0.toString)
    row.put("V27", 0.toString)
    row.put("V28", 0.toString)
    row.put("Amount", record.amount.toString)

    row
  }

  /** Score a record with the model */
  override protected def invokeModel(input: CustomerTransaction): Either[String, Double] = {
    val row = toRow(input)
    try {
      val prediction = h2oModel.predict(row).asInstanceOf[Double]
      Right(prediction)
    } catch {
      case t: Throwable ⇒ Left(t.getMessage)
    }
  }
}

/**
 * Factory for Fraud H2O model
 */
object FraudH2OModelFactory extends ModelFactory[CustomerTransaction, Double] {

  protected def make(descriptor: ModelDescriptor): Either[String, Model[CustomerTransaction, Double]] =
    Right(new FraudH20Model(descriptor))
}
