package reactivestreams.akkastreams

import cats.data.Xor
import io.circe.generic.auto._
import io.circe.parser._

/**
  * Created by walter
  */
object BitcoinModels extends App {
  case class TransactionInput(
    sequence: Long,
    prev_out: TransactionOutput,
    script: String)
  case class TransactionOutput(
    spent: Boolean,
    tx_index: Long,
    `type`: Int,
    addr: String,
    value: Long,
    n: Int,
    script: String)
  case class Transaction(
    lock_time: Int,
    ver: Int,
    size: Int,
    time: Long,
    tx_index: Long,
    hash: String,
    relayed_by: String,
    vin_sz: Int,
    vout_sz: Int,
    inputs: List[TransactionInput],
    out: List[TransactionOutput])
  case class UnconfirmedTransaction(
    op: String,
    x: Transaction)
  def decodeUnconfirmedTransaction(json: String): Xor[Throwable, UnconfirmedTransaction] = decode[UnconfirmedTransaction](json).leftMap(new Exception(_))
}
