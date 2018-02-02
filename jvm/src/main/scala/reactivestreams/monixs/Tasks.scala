package reactivestreams.monixs

import cats._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Created by walter
  */
object Tasks extends App {
  val task1 = Task { Thread.sleep(2000); 1 }
  val task2 = Task { Thread.sleep(2000); 2 }
  val task3 = Task { Thread.sleep(2000); 3 }
  println("starting...")
  val ts1 = for {
    x <- task1
    y <- task2
    z <- task3
  } yield x + y + z
  val ts2 = Task.zip3(task1, task2, task3) map { case (x, y, z) => x + y + z }
  ts1.runAsync.foreach(println)
  StdIn.readLine
}
