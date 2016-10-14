package reactivestreams

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.Try
import AkkaHttp._
import akka.NotUsed
import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.server.Directives._
import akka.stream.{Attributes, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Source, Zip}

/**
  * Created by walter
  */
object Firehose {
  def main(args: Array[String]): Unit = {
    val delay = Try(args(0).toInt).getOrElse(0)
    val route =
      pathSingleSlash {
        get {
          complete(HttpEntity.Chunked(
            ContentTypes.`application/json`,
            jsonSource(delay)
              .via(syncLogger("firehose"))
              .withAttributes(Attributes.logLevels(Logging.DebugLevel))
              .map(ChunkStreamPart(_))))
        }
      }
    val binding = startServer(route)
    //binding.flatMap(_.unbind).onComplete(_ => system.terminate)
  }
  def jsonSource(delay: Int): Source[String, NotUsed] = Source.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val zip = b.add(Zip[Unit, String]())
    val tick = if (delay > 0) Source.tick(Duration.Zero, Duration(delay, TimeUnit.MILLISECONDS), ()) else Source.repeat(())
    val time = Source.unfold(System.currentTimeMillis)(t => Some((System.currentTimeMillis, t)))
    val json = time.map(t => s"""{"name": "walter", "age": 18, "timestamp": $t}""")
    tick ~> zip.in0
    json ~> zip.in1
    SourceShape(zip.out.map(_._2).outlet)
  })
}
