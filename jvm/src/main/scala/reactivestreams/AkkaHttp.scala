package reactivestreams

import akka.NotUsed
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ws.{WebSocketRequest, Message}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{Http, server}
import akka.stream.scaladsl.Flow
import scala.concurrent.Future

/**
  * Created by walter
  */
object AkkaHttp extends AkkaImplicits {
  def startServer(route: Route, host: String = "0.0.0.0", port: Int = 8080): Future[ServerBinding] = Http().bindAndHandle(route, host, port)
  def sendServer(request: HttpRequest): Future[HttpResponse] = Http().singleRequest(request)
  def serverFlow[A] = Http().superPool[A]()
  def clientWebSocket[M](request: WebSocketRequest, flow: Flow[Message, Message, M]) = Http().singleWebSocketRequest(request, flow)
  def clientWebSocketFlow(request: WebSocketRequest) = Http().webSocketClientFlow(request)
  def syncLogger(prefix: String): Flow[String, String, NotUsed] = Flow fromFunction { x => println(s"prefix: $x"); x }
}
