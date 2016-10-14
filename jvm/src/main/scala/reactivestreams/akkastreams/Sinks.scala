package reactivestreams.akkastreams

import akka.stream.scaladsl.{Sink, SinkQueueWithCancel}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by walter
  */
object Sinks {
  trait Receptor[A] {
    def receive: Future[Option[A]]
    def close: Unit
  }
  def receptor[A](implicit ec: ExecutionContext): Sink[A, Receptor[A]] =
    Sink
      .queue[A]
      .mapMaterializedValue(q => new Receptor[A] {
        def receive = q.pull
        def close = q.cancel
      })
}
