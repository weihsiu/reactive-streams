package reactivestreams.akkastreams

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import reactivestreams.AkkaHttp._
import Bitcoins._
import scala.io.StdIn

/**
  * Created by walter
  */
object BitcoinTransactions extends App {
  def echoFlow: Flow[Message, Message, _] = Flow[Message].map(identity)
  def transactionIPInfoFlow: Flow[Message, Message, _] =
    Flow.fromSinkAndSource(Sink.ignore, transactionIPInfoSource.map(TextMessage.Strict))
  startServer(
    path("transaction-ip-info") {
      handleWebSocketMessages(transactionIPInfoFlow)
    }
  )

  StdIn.readLine
  system.terminate
}
