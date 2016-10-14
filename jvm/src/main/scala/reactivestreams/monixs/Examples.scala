package reactivestreams.monixs

import akka.stream.scaladsl.Sink
import java.nio.file.Paths
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observers.Subscriber
import monix.reactive._
import reactivestreams.{AkkaHttp, AkkaImplicits}
import reactivestreams.akkastreams.Sources
import scala.io.StdIn
import scala.concurrent.duration._
import scala.util.{Random, Success}

/**
  * Created by walter
  */
object Examples extends App with AkkaImplicits {
  def example0 = Observable
    .fromIterator(Iterator.continually(Random.nextDouble))
    .bufferSliding(2, 2)
    .take(10000)
    .map { case Seq(x, y) => if (x * x + y * y < 1.0) 1 else 0 }
    .runWith(Consumer.foldLeft((0, 0)) { case ((in, total), x) => (in + x, total + 1) })
    .runAsync(_.get match { case (in, total) => println(s"pi = ${4.0 * in / total}") })
  def example1 = Observable
    .zip2(
      Observable.intervalAtFixedRate(1.second),
      Observable.fromReactivePublisher(Sources.stockTrades("TPE", "2330").runWith(Sink.asPublisher(false)))
    )
    .map(_._2)
    .filter(t => Math.abs(t.changePercentage) > 3.0)
    .dump("")
    .take(30)
    .subscribe
  def example2 = Observables
    .stockPrices("TPE", "2330", 86400, "11d")
    .map(_.toString + "\n")
    .runWith(Consumers.writeFile(Paths.get("data/tsmc")))
    .runAsync
  def example3 = Observable
    .fromReactivePublisher(Sources.fromInt(0).runWith(Sink.asPublisher(false)))
    .filter(_ % 2 == 0)
    .take(10)
    .runWith(Consumer.foreach(println))
    .runAsync
  def example4 = Observable
    .fromIterable(1 to 10)
    .map { n => println("1" * n); n }
//    .asyncBoundary(OverflowStrategy.Default)
    .map { n => println("2" * n); n }
//    .asyncBoundary(OverflowStrategy.Default)
    .map { n => println("3" * n); n }
//    .asyncBoundary(OverflowStrategy.Default)
    .subscribe
  def example5 = {
    // cold observable
    val observable = Observable.intervalAtFixedRate(1.second).take(10)
    observable.subscribe(Observer.dump("1"))
    Thread.sleep(5000)
    observable.subscribe(Observer.dump("2"))
  }
  def example6 = {
    // hot observable
    val observable = Observable.intervalAtFixedRate(1.second).take(10).publish
    observable.connect
    observable.subscribe(Observer.dump("1"))
    Thread.sleep(5000)
    observable.subscribe(Observer.dump("2"))
  }

  example1
  StdIn.readLine
  AkkaHttp.system.terminate
  system.terminate
}
