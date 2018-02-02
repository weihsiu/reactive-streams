package reactivestreams.akkastreams

import enumeratum._
import io.circe.generic.auto._
import io.circe.jawn._

/**
  * Created by walter
  */
object BitcoinModels extends App {

  sealed trait Preference extends EnumEntry

  case object Preference extends Enum[Preference] with CirceEnum[Preference] {

    case object high extends Preference

    case object medium extends Preference

    case object low extends Preference

    val values = findValues
  }

  case class UnconfirmedTransaction(
                                     block_height: Int, hash: String,
                                     addresses: List[String],
                                     total: Long, fees: Int, size: Int,
                                     // Enum - high, medium, low
                                     preference: Preference,
                                     relayed_by: String, received: String,
                                     ver: Int,
                                     //lock_time: Int, Option
                                     double_spend: Boolean,
                                     vin_sz: Int, vout_sz: Int,
                                     confirmations: Int,
                                     inputs: List[TransactionInput], outputs: List[TransactionOutput],
                                   )

  case class TransactionInput(
                               prev_hash: String, output_index: Int,
                               output_value: Long, script_type: String,
                               script: String, addresses: List[String],
                               sequence: Long,
                             )

  case class TransactionOutput(
                                value: Long, script: String,
                                addresses: List[String],
                                script_type: String
                              )

  def decodeUnconfirmedTransaction(json: String): Either[Throwable, UnconfirmedTransaction] = decode[UnconfirmedTransaction](json).left.map(new Exception(_))

}
