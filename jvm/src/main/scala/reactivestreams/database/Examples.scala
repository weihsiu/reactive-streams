package reactivestreams.database

import cats.implicits._
import fs2.interop.cats._
import fs2.{Strategy, Task}
import reactivestreams.AkkaImplicits

object Examples extends App with AkkaImplicits {
    val r = for {
      _ <- Streams.userSource.runForeach(println)
      _ <- Streams.userSource.map(u => u.copy(id = None)).via(Streams.insertUserFlow).runForeach(println)
      _ <- Streams.userSource.runForeach(println)
    } {}

//  val r =
//    Streams.userSource.runForeach(println) >>
//    Streams.userSource.map(u => u.copy(id = None)).via(Streams.insertUserFlow).runForeach(println) >>
//    Streams.userSource.runForeach(println)

//  implicit val strategy = Strategy.fromFixedDaemonPool(8)
//  val r =
//    Task.fromFuture(Streams.userSource.runForeach(println)) >>
//    Task.fromFuture(Streams.userSource.map(u => u.copy(id = None)).via(Streams.insertUserFlow).runForeach(println)) >>
//    Task.fromFuture(Streams.userSource.runForeach(println))
//  r.unsafeRun

  Thread.sleep(1000)
  system.terminate
}
