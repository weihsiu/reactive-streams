package reactivestreams.akkastreams

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import reactivestreams.WebServices._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by walter
  */
object Sources {
  def fromInt(from: Int): Source[Int, NotUsed] =
    Source.unfold(from)(s => Some((s + 1, s)))
//  def fromSink[A, M](sink: Sink[A, M]): Source[A, M] = Source.queue(0, OverflowStrategy.fail) mapMaterializedValue { queue =>
//
//  }
  trait Emitter[A] {
    def emit(x: A): Unit
    def close: Unit
  }
  def emitter[A]: Source[A, Emitter[A]] =
    Source
      .queue[A](0, OverflowStrategy.fail)
      .mapMaterializedValue(q => new Emitter[A] {
        def emit(x: A) = q.offer(x)
        def close = q.complete
      })
  def stockTrade(exchange: String, symbol: String): Source[StockTrade, NotUsed] =
    Source
      .fromFuture(retrieveStockTrade(exchange, symbol))
      .flatMapConcat(_.fold(Source.failed(_), Source.single(_)))
  def stockTrades(exchange: String, symbol: String): Source[StockTrade, NotUsed] =
    Source
      .unfoldAsync(())(_ =>
        retrieveStockTrade(exchange, symbol)
          .map(_.toOption.map(((), _))))
}
