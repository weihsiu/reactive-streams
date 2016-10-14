package reactivestreams.akkastreams

import akka.stream._
import akka.stream.scaladsl.{BidiFlow, Flow, GraphDSL, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import java.nio.ByteOrder
import reactivestreams.AkkaImplicits

/**
  * Created by walter
  */
object GraphStages extends AkkaImplicits with App {
  sealed trait Message
  case class Ping(id: Int) extends Message
  case class Pong(id: Int) extends Message
  def toBytes(msg: Message): ByteString = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    msg match {
      case Ping(id) => ByteString.newBuilder.putByte(1).putInt(id).result
      case Pong(id) => ByteString.newBuilder.putByte(2).putInt(id).result
    }
  }
  def fromBytes(bs: ByteString): Message = {
    implicit val order = ByteOrder.LITTLE_ENDIAN
    val it = bs.iterator
    it.getByte match {
      case 1 => Ping(it.getInt)
      case 2 => Pong(it.getInt)
      case n => sys.error(s"invalid message type: $n")
    }
  }
  val codec = BidiFlow.fromFunctions(toBytes _, fromBytes _)
  val pingPong = Flow[Message] collect { case Ping(id) => Pong(id) }
  val flow1 = codec.atop(codec.reversed).join(pingPong)
  Source(0 to 9).map(Ping).via(flow1).runForeach(println)

  val framing = BidiFlow.fromGraph(GraphDSL.create() { b =>
    implicit val order = ByteOrder.LITTLE_ENDIAN
    def addLengthHeader(bs: ByteString) = ByteString.newBuilder.putInt(bs.length).append(bs).result
    class FrameParser extends GraphStage[FlowShape[ByteString, ByteString]] {
      val in = Inlet[ByteString]("FrameParser.in")
      val out = Outlet[ByteString]("FrameParser.out")
      val shape = FlowShape(in, out)
      def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
        var stash = ByteString.empty
        var needed = -1
        setHandler(out, new OutHandler {
          def onPull = if (isClosed(in)) run else pull(in)
        })
        setHandler(in, new InHandler {
          def onPush = {
            val bs = grab(in)
            stash ++= bs
            run
          }
          def onUpStreamFinish = if (stash.isEmpty) completeStage else if (isAvailable(out)) run
        })
        def run: Unit = {
          if (needed == -1) {
            if (stash.length < 4) {
              if (isClosed(in)) completeStage else pull(in)
            } else {
              needed = stash.iterator.getInt
              stash = stash.drop(4)
              run
            }
          } else if (stash.length < needed) {
            if (isClosed(in)) completeStage else pull(in)
          } else {
            val emit = stash.take(needed)
            stash = stash.drop(needed)
            needed = -1
            push(out, emit)
          }
        }
      }
    }
    val outbound = b.add(Flow[ByteString].map(addLengthHeader))
    val inbound = b.add(Flow[ByteString].via(new FrameParser))
    BidiShape.fromFlows(outbound, inbound)
  })
  val stack = codec.atop(framing)
  val flow2 = stack.atop(stack.reversed).join(pingPong)
  Source(0 to 9).map(Ping).via(flow2).runForeach(println)

  Thread.sleep(3000)
  system.terminate
}
