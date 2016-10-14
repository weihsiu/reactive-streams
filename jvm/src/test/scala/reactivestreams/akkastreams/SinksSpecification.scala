package reactivestreams.akkastreams

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSource
import org.scalacheck.Prop.{BooleanOperators, forAll}
import org.scalacheck.Properties
import reactivestreams.AkkaImplicits
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by walter
  */
object SinksSpecification extends Properties("Sinks") with AkkaImplicits {
  import Sinks._
  property("receptor1") = forAll { x: Int =>
    val (p, r) = TestSource.probe[Int].toMat(receptor[Int])(Keep.both).run
    p.sendNext(x)
    p.sendComplete
    Await.result(r.receive, 3.seconds) == Some(x) &&
    Await.result(r.receive, 3.seconds) == None
  }
  property("receptor2") = forAll { s: Source[String, NotUsed] =>
    val ss = s.runFold(Vector.empty[String])(_ :+ _)
    val r = s.runWith(receptor[String])
    Await.result(ss, 3.seconds).forall(_ == Await.result(r.receive, 3.seconds).get) &&
    Await.result(r.receive, 3.seconds) == None
  }
}
