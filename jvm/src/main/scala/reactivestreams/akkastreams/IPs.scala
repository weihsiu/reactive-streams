package reactivestreams.akkastreams

import java.net.URLEncoder

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.Flow
import cats.implicits._
import reactivestreams.AkkaHttp._
import reactivestreams.IPModels._

import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Created by walter
  */
object IPs extends App {
  def getEntityText(response: HttpResponse): Future[String] = response.entity.dataBytes.runReduce(_ ++ _).map(_.utf8String)

  def ipInfoFlow: Flow[String, Either[Throwable, IPInfo], NotUsed] =
    Flow[String]
      .throttle(120, 1.minute, 1, Shaping)
      .map(x => (HttpRequest(uri = s"""http://ip-api.com/json/${URLEncoder.encode(x, "utf-8")}""") -> 42))
      .via(serverFlow[Int])
      .map { r => Either.fromTry(r._1) }
      .mapAsync(4)(_.traverse(getEntityText(_)))
      .map(_.flatMap(decodeIPInfo(_)))

}
