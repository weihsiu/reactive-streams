package reactivestreams.monixs

import monix.reactive._
import reactivestreams.WebServices._

/**
  * Created by walter
  */
object Observables {
  def fromInt(from: Int): Observable[Int] =
    Observable.fromStateAction[Int, Int](s => (s, s + 1))(from)
  def stockPriceHistory(exchange: String, symbol: String, interval: Int, period: String): Observable[StockPriceHistory] =
    Observable.fromFuture(retrieveStockPriceHistory(exchange, symbol, interval, period))
      .flatMap(_.fold(Observable.raiseError(_), Observable.now(_)))
  def stockPrices(exchange: String, symbol: String, interval: Int, period: String): Observable[StockPrice] =
    stockPriceHistory(exchange, symbol, interval, period)
      .flatMap(h => Observable.fromIterable(h.stockPrices))
}
