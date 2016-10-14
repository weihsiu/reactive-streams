package reactivestreams.monixs

import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.{Consumer, Observer}

/**
  * Created by walter
  */
object Consumers {
  def writeFile(path: Path): Consumer[String, Unit] =
    Consumer.fromObserver[String] { implicit scheduler =>
      new Observer.Sync[String] {
        val writer = Files.newBufferedWriter(path, Charset.forName("utf-8"))
        def onNext(x: String): Ack = {
          writer.write(x)
          Continue
        }
        def onComplete = writer.close
        def onError(e: Throwable) = writer.close
      }
    }
}
