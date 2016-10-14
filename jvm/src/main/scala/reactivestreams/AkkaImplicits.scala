package reactivestreams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

/**
  * Created by walter
  */
trait AkkaImplicits {
  val config = ConfigFactory.parseString(
    """
      |akka.loglevel = debug""".stripMargin)
  implicit val system = ActorSystem("reactive-streams", config, classLoader = getClass.getClassLoader)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
}
