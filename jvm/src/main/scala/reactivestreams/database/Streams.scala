package reactivestreams.database

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.H2Driver.api._

object Streams {
  val userSource: Source[H2Database.User, NotUsed] =
    Source
      .fromFuture(H2Database.database)
      .flatMapConcat(db => Source.fromPublisher(db.stream(H2Database.users.result)))
  val insertUserFlow: Flow[H2Database.User, Int, NotUsed] =
    Flow[H2Database.User].mapAsync(1)(user =>
      for {
        db <-  H2Database.database
        id <- db.run((H2Database.users returning H2Database.users.map(_.id)) += user)
      } yield id
    )
}
