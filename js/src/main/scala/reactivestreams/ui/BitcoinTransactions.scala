package reactivestreams.ui

import monix.reactive.Consumer
import monix.execution.Scheduler.Implicits.{global => _global}
import org.scalajs.dom
import org.scalajs.dom.html.Element
import reactivestreams.IPModels._
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.Dynamic.{global, literal}

/**
  * Created by walter
  */
@JSExportTopLevel("reactivestreams.ui.BitcoinTransactions")
object BitcoinTransactions {
  @JSExport
  def main(mapDiv: Element): Unit = {
    val map = js.Dynamic.newInstance(global.google.maps.Map)(mapDiv, literal(
      center = literal(lat = 46.8182, lng = 8.2275),
      zoom = 3
    ))

    Observables
      .webSocketMessage("ws://localhost:8080/transaction-ip-info")
      .map(decodeIPInfo)
      .filter(_.isRight)
      .map(_.getOrElse(sys.error("wtf")))
      .foreach { ipInfo =>
        js.Dynamic.newInstance(global.google.maps.Marker)(literal(
          position = literal(lat = ipInfo.lat, lng = ipInfo.lon),
          map = map,
          title = s"${ipInfo.city}, ${ipInfo.country}"
        ))
      }
  }
}
