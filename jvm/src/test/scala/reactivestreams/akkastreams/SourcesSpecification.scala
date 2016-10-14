package reactivestreams.akkastreams

import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSink
import org.scalacheck.Properties
import org.scalacheck.Prop.{forAll, BooleanOperators}
import reactivestreams.AkkaImplicits
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by walter
  */
object SourcesSpecification extends Properties("Sources") with AkkaImplicits {
  import Sources._
  property("emmiter1") = forAll { x: Int =>
    val (e, v) = emitter[Int].toMat(Sink.head)(Keep.both).run
    e.emit(x)
    e.close
    Await.result(v, 3.seconds) == x
  }
  property("emmiter2") = forAll { x: Int =>
    val (e, p) = emitter[Int].toMat(TestSink.probe[Int])(Keep.both).run
    e.emit(x)
    e.close
    Try(p.request(1).expectNext(x).expectComplete).isSuccess :| "failed hahaha"
  }
}
