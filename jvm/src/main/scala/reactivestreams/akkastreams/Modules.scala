package reactivestreams.akkastreams

import akka.stream
import akka.stream._
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import reactivestreams.AkkaImplicits

/**
  * Created by walter
  */
object Modules extends AkkaImplicits with App {
  val g1 = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val A: Outlet[Int] = b.add(Source.single(0)).out
    val B: UniformFanOutShape[Int, Int] = b.add(Broadcast[Int](2))
    val C: UniformFanInShape[Int, Int] = b.add(Merge[Int](2))
    val D: FlowShape[Int, Int] = b.add(Flow[Int].map(_ + 1))
    val E: UniformFanOutShape[Int, Int] = b.add(Balance[Int](2))
    val F: UniformFanInShape[Int, Int] = b.add(Merge[Int](2))
    val G: Inlet[Any] = b.add(Sink.foreach(println)).in
              C   <~    F
    A ~> B ~> C   ~>    F
         B ~> D ~> E ~> F
                   E ~> G
    ClosedShape
  })
  g1.run

  Thread.sleep(3000)
  system.terminate
}
