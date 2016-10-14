package reactivestreams

import AkkaHttp._
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Sink

/**
  * watch -d -n 1 netstat -n -p tcp
  */
object DeepWell {
  def main(args: Array[String]): Unit = {
    sendServer(HttpRequest(uri = "http://localhost:8080/")) foreach { response =>
      response.entity.withoutSizeLimit.asInstanceOf[Chunked]
        .chunks
        .map(_.data.utf8String)
        .via(syncLogger("deepwell"))
        .runWith(Sink.ignore)
    }
  }
}
