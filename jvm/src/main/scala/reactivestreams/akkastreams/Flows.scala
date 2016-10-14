package reactivestreams.akkastreams

import akka.actor.PoisonPill
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.typed._
import akka.typed.adapter._
import akka.typed.ScalaDSL._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Created by walter
  */
object Flows {
  trait Trigger {
    def trigger: Future[Unit]
  }
  def trigger[A](implicit ec: ExecutionContext): Flow[A, A, Trigger] = {
    val sink = Sink.queue[A]
    val source = Source.queue[A](0, OverflowStrategy.fail)
    Flow.fromSinkAndSourceMat(sink, source)((sinkQueue, sourceQueue) => new Trigger {
      def trigger = {
        val p = Promise[Unit]
        sinkQueue.pull andThen {
          case Success(x) => x.fold(sourceQueue.complete)(sourceQueue.offer(_))
          case Failure(e) => sourceQueue.fail(e)
        } andThen {
          case _ => p.complete(Try(()))
        }
        p.future
      }
    })
  }
  trait Toggle {
    def toggle(on: Boolean): Unit
  }
  def toggle[A](implicit ec: ExecutionContext): Flow[A, A, Toggle] = {
    val sink = Sink.queue[A]
    val source = Source.queue[A](0, OverflowStrategy.fail)
    Flow.fromSinkAndSourceMat(sink, source)((sinkQueue, sourceQueue) => new Toggle {
      @volatile var on = true
      def loop: Unit = if (on) {
        sinkQueue.pull onComplete {
          case Success(x) => x.fold(sourceQueue.complete) { y =>
            sourceQueue.offer(y)
            loop
          }
          case Failure(e) => sourceQueue.fail(e)
        }
      }
      loop
      def toggle(on: Boolean) = {
        this.on = on
        loop
      }
    })
  }
  def toggle2[A](implicit untypedActorSystem: akka.actor.ActorSystem): Flow[A, A, Toggle] = {
    implicit val ec = untypedActorSystem.dispatcher
    Flow.fromSinkAndSourceMat(Sink.queue[A], Source.queue[A](0, OverflowStrategy.fail))((sinkQueue, sourceQueue) => new Toggle {
      trait Message
      case class Toggle(on: Boolean) extends Message
      case object Handover extends Message
      def toggler(on: Boolean): Behavior[Message] = Full[Message] {
        case Sig(ctx, PreStart) =>
          ctx.self ! Handover
          Same
        case Msg(ctx, Handover) =>
          if (on) {
            sinkQueue.pull.onComplete {
              case Success(x) => x.fold(sourceQueue.complete) { y =>
                sourceQueue.offer(y)
                ctx.self ! Handover
              }
              case Failure(e) => sourceQueue.fail(e)
            }
          }
          Same
        case Msg(ctx, Toggle(on)) =>
          ctx.self ! Handover
          toggler(on)
      }
      // http://stackoverflow.com/questions/31621607/how-do-i-mix-typed-and-untyped-actors
      val t = untypedActorSystem.spawn(toggler(true), "toggler")
      def toggle(on: Boolean) = t ! Toggle(on)
    })
  }
}
