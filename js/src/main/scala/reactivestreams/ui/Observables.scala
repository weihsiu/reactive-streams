package reactivestreams.ui

import monix.execution.Cancelable
import monix.reactive.{Observable, OverflowStrategy}
import org.scalajs.dom

/**
  * Created by walter
  */
object Observables {
  def webSocketMessage(url: String): Observable[String] = {
    Observable.unsafeCreate { subscriber =>
      val socket = new dom.WebSocket(url)
      socket.onmessage = (e: dom.MessageEvent) => subscriber.onNext(e.data.toString)
      socket.onerror = (e: dom.ErrorEvent) => subscriber.onError(new Exception(e.message))
      socket.onclose = (e: dom.CloseEvent) => subscriber.onComplete()
      Cancelable(() => socket.close(0, "cancel"))
    }
  }
}
