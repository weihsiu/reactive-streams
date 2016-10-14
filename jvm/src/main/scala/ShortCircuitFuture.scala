import cats.data.{OptionT, XorT}
import cats.implicits._
import fs2.interop.cats._
import fs2.{Strategy, Task}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Created by walter
  */
object ShortCircuitFuture extends App {
  def async1(x: Boolean): Future[Boolean] = {
    println("async1")
    Future.successful(x)
  }

  def async2(x: Boolean): Future[Boolean] = {
    println("async2")
    Future.successful(x)
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  val result1: Future[Boolean] = for {
    r1 <- async1(false)
    r2 <- if (r1) async2(true) else Future.successful(false)
  } yield r2

  println(Await.result(result1, 3.seconds))

  implicit val strategy = Strategy.fromExecutionContext(global)

  val result2 = for {
    r1 <- Task.fromFuture(async1(false))
    r2 <- Task.fromFuture(async2(true))
  } yield r2

//  println(s"result is ${result2.unsafeRun}")

  val result3: Future[Option[Boolean]] = (for {
    r1 <- OptionT(async1(false).map(if (_) Some(true) else None))
    r2 <- OptionT(async2(true).map(if (_) Some(true) else None)) if r1
  } yield r2).value

  println(Await.result(result3, 3.seconds))

}
