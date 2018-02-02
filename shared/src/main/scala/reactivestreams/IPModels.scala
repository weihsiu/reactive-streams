package reactivestreams

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
/**
  * Created by walter
  */
object IPModels {
  case class IPInfo(
    country: String,
    countryCode: String,
    region: String,
    regionName: String,
    city: String,
    zip: String,
    lat: Double,
    lon: Double,
    timezone: String,
    isp: String,
    org: String,
    as: String,
    query: String)
  def decodeIPInfo(json: String): Either[Throwable, IPInfo] = decode[IPInfo](json).left.map(new Exception(_))
  def encodeIPInfo(ipInfo: IPInfo): String = ipInfo.asJson.noSpaces
}
