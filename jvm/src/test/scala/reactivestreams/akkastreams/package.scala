package reactivestreams

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import scala.collection.immutable.Iterable

/**
  * Created by walter
  */
package object akkastreams {
  def sourceGen[A : Arbitrary](count: Int): Gen[Source[A, NotUsed]] =
    Gen.buildableOfN[Iterable[A], A](count, implicitly[Arbitrary[A]].arbitrary).map(Source.apply)
  implicit def arbitrarySource[A : Arbitrary]: Arbitrary[Source[A, NotUsed]] = Arbitrary(sourceGen[A](10))
}
