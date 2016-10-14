package reactivestreams.akkastreams

import reactivestreams.{AkkaHttp, AkkaImplicits}
import Flows._
import Sources._
import akka.{Done, NotUsed}
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source, ZipWith}
import monix.reactive.Observable
import reactivestreams.monixs.Observables
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Random

/**
  * Created by walter
  */
object Examples extends App with AkkaImplicits {
  // https://scalafiddle.io/sf/uLRJMF8/1
  def example0 = {
    Source
      .fromIterator(() => Iterator.continually(Random.nextDouble))
      .grouped(2)
      .take(10000)
      .map { case Seq(x, y) => if (x * x + y * y < 1.0) 1 else 0 }
      .fold((0, 0)) { case ((in, total), x) => (in + x, total + 1) }
      .runForeach { case (in, total) => println(s"pi = ${4.0 * in / total}") }
  }
  def example1 = stockTrade("TPE", "2330").runForeach(println)
  def example2 = Source
    .tick(0.second, 1.second, ())
    .zip(stockTrades("TPE", "2330"))
    .log("example2")
    .map(_._2)
    .filter(t => Math.abs(t.changePercentage) > 3.0)
    .runForeach(println)
  def example3 = {
    val g = fromInt(1)
      .viaMat(trigger)(Keep.right)
      .to(Sink.foreach(println))
    val t = g.run
    Source.tick(1.second, 1.second, ()).runForeach(_ => t.trigger)
  }
  def example4 = {
    val g = fromInt(1)
      .viaMat(toggle2)(Keep.right)
      .to(Sink.foreach(println))
    val t = g.run
    Thread.sleep(1000)
    t.toggle(false)
    Thread.sleep(1000)
    t.toggle(true)
  }
  def example5 = {
    import monix.execution.Scheduler.Implicits.global
    Source
      .fromPublisher(Observables.fromInt(0).toReactivePublisher)
      .filter(_ % 2 == 0)
      .take(10)
      .runForeach(println)
  }
  def example6 = Source(1 to 10)
    .map { n => println("1" * n); n }
    .async
    .map { n => println("2" * n); n }
    .async
    .map { n => println("3" * n); n }
    .async
    .runWith(Sink.ignore)
  def example7 = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val source = Source.repeat(1)
      val start = Source.single(0)
      val zip = b.add(ZipWith((left: Int, right: Int) => left + right))
      val broadcast = b.add(Broadcast[Int](2))
      val concat = b.add(Concat[Int]())
      source ~> zip.in0
                zip.out.map { x => println(x); x } ~> broadcast ~> Sink.ignore
                zip.in1 <~ concat <~ start
                           concat <~ broadcast
      ClosedShape
    }).run
  }

  example2
  StdIn.readLine
  AkkaHttp.system.terminate
  system.terminate
}
