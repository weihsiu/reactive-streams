package reactivestreams

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import io.circe.Decoder
import io.circe.parser._
import java.io.StringReader
import java.net.URLDecoder
import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Properties
import reactivestreams.AkkaHttp._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try
import cats.implicits._

/**
  * Created by walter
  */
object WebServices extends App {
  case class StockPrice(
    exchange: String,
    symbol: String,
    dateTime: ZonedDateTime,
    close: Double,
    high: Double,
    low: Double,
    open: Double,
    volume: Int)
  case class StockPriceHistory(
    exchange: String,
    marketOpenMinute: Int,
    marketCloseMinute: Int,
    interval: Int,
    timezoneOffset: Int,
    stockPrices: Vector[StockPrice])
  def retrieveStockPriceHistory(exchange: String, symbol: String, interval: Int, period: String): Future[Either[Throwable, StockPriceHistory]] = {
    def processPrice(interval: Int, timezoneOffset: Int)(acc: (ZonedDateTime, Vector[StockPrice]), line: String): (ZonedDateTime, Vector[StockPrice]) = acc match {
      case (base, prices) =>
        val ps = line.split(',')
        val (newBase, dateTime) =
          if (ps(0).startsWith("a")) {
            val dt = ZonedDateTime.ofInstant(
              Instant.ofEpochSecond(ps(0).tail.toInt),
              ZoneId.from(ZoneOffset.ofTotalSeconds(timezoneOffset * 60)))
            (dt, dt)
          } else (base, base.plusSeconds(ps(0).toInt * interval))
        (newBase, prices :+ StockPrice(exchange, symbol, dateTime, ps(1).toDouble, ps(2).toDouble, ps(3).toDouble, ps(4).toDouble, ps(5).toInt))
    }
    def toHistory(data: String): Either[Throwable, StockPriceHistory] = {
      Either.fromTry(Try {
        val decoded = URLDecoder.decode(data, "ascii")
        val (info, history) = decoded.split('\n').splitAt(7)
        val ps = new Properties
        ps.load(new StringReader(info.mkString("\n")))
        val interval = ps.getProperty("INTERVAL").toInt
        val timezoneOffset = ps.getProperty("TIMEZONE_OFFSET").toInt
        StockPriceHistory(
          ps.getProperty("EXCHANGE"),
          ps.getProperty("MARKET_OPEN_MINUTE").toInt,
          ps.getProperty("MARKET_CLOSE_MINUTE").toInt,
          interval,
          timezoneOffset,
          history.foldLeft((ZonedDateTime.now, Vector.empty[StockPrice]))(processPrice(interval, timezoneOffset))._2)
      })
    }
    implicit val stockPriceHistoryUnmarshaller = Unmarshaller.stringUnmarshaller.map(toHistory)
    sendServer(HttpRequest(
      uri = Uri("http://www.google.com/finance/getprices?f=d,c,h,l,o,v")
        .withQuery(
          Query(
            "x" -> exchange,
            "q" -> symbol,
            "i" -> interval.toString,
            "p" -> period)))) flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[Either[Throwable, StockPriceHistory]]
//        Unmarshal(entity).to[String].map(data => toHistory(data))
      case HttpResponse(status, _, _, _) =>
        Future.successful(Either.left(new Exception(s"server returns $status")))
    }
  }
  case class StockTrade(
    exchange: String, // e
    symbol: String, // t
    currency: String,
    lastTradePrice: Double, // l
    lastTradeTime: ZonedDateTime, // lt
    lastTradeSize: Int, // s
    change: Double, // c
    changePercentage: Double, // cp
    previousClosedPrice: Double)
  def retrieveStockTrade(exchange: String, symbol: String): Future[Either[Throwable, StockTrade]] = {
    def toTrade(data: String): Either[Throwable, StockTrade] = {
      implicit val stockTradeDecoder: Decoder[StockTrade] = Decoder.decodeJson emapTry { json =>
        def extractCurrency(price: String): String = price.split('$')(0)
        Try {
          val trade = json.asArray.get.head.asObject.get
          StockTrade(
            trade("e").get.asString.get,
            trade("t").get.asString.get,
            extractCurrency(trade("l_cur").get.asString.get),
            trade("l").get.asString.get.toDouble,
            ZonedDateTime.parse(trade("lt_dts").get.asString.get),
            trade("s").get.asString.get.toInt,
            trade("c").get.asString.get.toDouble,
            trade("cp").get.asString.get.toDouble,
            trade("pcls_fix").get.asString.get.toDouble)
        }
      }
      decode[StockTrade](data)
    }
    implicit val stockTradeUnmarshaller = Unmarshaller.stringUnmarshaller.map(d => toTrade(d.drop(3)))
    sendServer(HttpRequest(
      uri = Uri("https://www.google.com/finance/info")
        .withQuery(Query("q" -> s"$exchange:$symbol")))) flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[Either[Throwable, StockTrade]]
      case HttpResponse(status, _, _, _) =>
        Future.successful(Either.left(new Exception(s"server teturn $status")))
    }
  }
  val h = retrieveStockTrade("TPE", "2330")
  println(Await.result(h, 10.seconds))
  system.terminate
}
