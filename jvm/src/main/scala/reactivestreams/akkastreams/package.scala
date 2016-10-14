package reactivestreams

import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.Materializer
import scala.concurrent.Future

/**
  * Created by walter
  */
package object akkastreams {
  def getText(message: TextMessage)(implicit materializer: Materializer): Future[String] =
    if (message.isStrict) Future.successful(message.getStrictText)
    else message.textStream.runReduce[String](_ + _)
}
