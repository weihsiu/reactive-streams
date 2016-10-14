package reactivestreams.akkastreams

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, Sink, Source}
import io.circe._
import reactivestreams.AkkaHttp._
import scala.concurrent.duration._
import scala.io.StdIn
import Sources._
import cats.data.Xor
import java.time.ZonedDateTime
import reactivestreams.akkastreams.BitcoinModels.UnconfirmedTransaction
import reactivestreams.IPModels._
import scala.concurrent.{Future, Promise}
import BitcoinModels._
import akka.http.scaladsl.model.ws.WebSocketUpgradeResponse
import akka.typed.ScalaDSL
import IPs._

/**
  * Created by walter
  */
object Bitcoins extends App {
  implicit class Piper[A](private val a: A) extends AnyVal {
    def |>[B](f: A => B) = f(a)
  }
  def dataFlow(webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]]): Flow[String, String, Future[WebSocketUpgradeResponse]] =
    Flow.fromGraph(GraphDSL.create(webSocketFlow.mapAsync(4)(m => getText(m.asInstanceOf[TextMessage]))) { implicit b => webSocketFlowS =>
      import GraphDSL.Implicits._
      val pingSourceS = b.add(Source.tick(Duration.Zero, 30.seconds, TextMessage.Strict("""{"op":"ping"}""")))
      val messageFlowS = b.add(Flow[String].map(TextMessage.Strict))
      val merge = b.add(Merge[Message](2))
      pingSourceS ~> merge ~> webSocketFlowS
      messageFlowS ~> merge
      FlowShape(messageFlowS.in, webSocketFlowS.out)
    })
  def dataFlow2(webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]]): Flow[String, String, Future[WebSocketUpgradeResponse]] =
    Flow[String]
      .map(TextMessage.Strict)
      .merge(Source.maybe) // a source that never completes will keep the downstream keepAlive from completing
      .keepAlive(30.seconds, () => TextMessage.Strict("""{"op":"ping"}"""))
      .viaMat(webSocketFlow)(Keep.right)
      .via(Flow[Message].mapAsync(4)(m => getText(m.asInstanceOf[TextMessage])))
  def unconfirmedTransactionSource(dataFlow: Flow[String, String, Future[WebSocketUpgradeResponse]]): Source[Xor[Throwable, UnconfirmedTransaction], Future[WebSocketUpgradeResponse]] =
    Source.single("""{"op":"unconfirmed_sub"}""").viaMat(dataFlow.map(decodeUnconfirmedTransaction))(Keep.right)
  def transactionIPInfoSource: Source[String, _] = (
    WebSocketRequest("wss://ws.blockchain.info/inv") |>
      clientWebSocketFlow |>
      dataFlow2 |>
      unconfirmedTransactionSource)
    .filter(_.isRight)
    .map(_.getOrElse(sys.error("wtf")).x.relayed_by)
    .filter(_ != "127.0.0.1")
    .via(ipInfoFlow)
    .map(_.map(encodeIPInfo))
    .filter(_.isRight)
    .map(_.getOrElse(sys.error("wtf")))
//  val r = unconfirmedTransactionSource(dataFlow2(clientWebSocketFlow(WebSocketRequest("wss://ws.blockchain.info/inv")))).runForeach(println)
  val r = transactionIPInfoSource.runForeach(println)
  r.foreach(println)

  StdIn.readLine
  system.terminate
}




















//  val flow1 = Flow.fromSinkAndSource(Sink.foreach(println), Source.tick(Duration.Zero, 1.seconds, TextMessage.Strict("""{"op":"ping"}""")))
//  def dataFlow[A, B](dataSource: Source[String, A], dataSink: Sink[Future[String], B]): Flow[Message, Message, (A, B)] =
//    Flow.fromGraph(
//      GraphDSL.create(
//        dataSource.map(TextMessage.Strict),
//        dataSink.contramap[Message](m => getText(m.asInstanceOf[TextMessage]))
//      )((_, _)) { implicit b => (sourceS, sinkS) =>
//        import GraphDSL.Implicits._
//        val pingSourceS = b.add(Source.tick(Duration.Zero, 30.seconds, TextMessage.Strict("""{"op":"ping"}""")))
//        val merge = b.add(Merge[Message](2))
//        pingSourceS ~> merge
//        sourceS ~> merge
//        FlowShape(sinkS.in, merge.out)
//      })
//  def unconfirmedTransactionFlow[A](unconfirmedTransactionSink: Sink[Future[Xor[Throwable, UnconfirmedTransaction]], A]): Flow[Message, Message, A] =
//    dataFlow(Source.single("""{"op":"unconfirmed_sub"}"""), unconfirmedTransactionSink.contramap(_.map(decodeUnconfirmedTransaction(_)))).mapMaterializedValue(_._2)
//  val dumpSink = Sink.foreach[Future[Xor[Throwable, UnconfirmedTransaction]]](_.foreach(println))
//  val (r, m) = clientWebSocket(
//    WebSocketRequest("wss://ws.blockchain.info/inv"),
//    unconfirmedTransactionFlow(dumpSink.contramap()))
//  r.foreach(println)