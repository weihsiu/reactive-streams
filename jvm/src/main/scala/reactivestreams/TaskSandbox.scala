package reactivestreams

import fs2.{Strategy, Task}
import scala.io.StdIn

/**
  * Created by walter
  */
object TaskSandbox extends App {
  def printThread = println(s"running in ${Thread.currentThread.getName}")
  printThread
  implicit val s = Strategy.fromFixedDaemonPool(8)
  val t1 = Task { printThread; println("t1"); 1 }
  println(t1.unsafeRun)
  val t2 = Task delay { printThread; println("t2"); 2 }
  println(t2.unsafeRun)
  val t3 = Task start t2
  val t4 = for {
    t <- t3
    n <- t
  } yield n
  println(t4.unsafeRun)
  StdIn.readLine
}

