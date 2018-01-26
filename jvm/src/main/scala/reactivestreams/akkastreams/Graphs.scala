package reactivestreams.akkastreams

import akka.NotUsed
import akka.stream.FanInShape.{Init, Name}
import akka.stream._
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}
import cats.implicits._
import reactivestreams.AkkaImplicits
import scala.collection.immutable
import scala.concurrent.Future

/**
  * Created by walter
  */
object Graphs extends AkkaImplicits with App {
  def test1 = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val in = Source(1 to 10)
      val out = Sink.foreach(println)
      val broadcast = b.add(Broadcast[Int](2))
      val merge = b.add(Merge[Int](2))
      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
      in ~> f1 ~> broadcast ~> f2 ~> merge ~> f3 ~> out
      broadcast ~> f4 ~> merge
      ClosedShape
    })
    g.run
  }
  def test2 = {
    val topSink = Sink.head[Int]
    val bottomSink = Sink.head[Int]
    val doubler = Flow[Int].map(_ * 2)
    val g = RunnableGraph.fromGraph(GraphDSL.create(topSink, bottomSink)((_, _)) {implicit b => (topSinkS, bottomSinkS) =>
      import GraphDSL.Implicits._
      val broadcast = b.add(Broadcast[Int](2))
      Source.single(1) ~> broadcast.in
      broadcast.out(0) ~> doubler ~> topSinkS.in
      broadcast.out(1) ~> doubler ~> doubler ~> bottomSinkS.in
      ClosedShape
    })
    g.run.bisequence.foreach(println)
  }
  def test3 = {
    val pickMax = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val zip1 = b.add(ZipWith[Int, Int, Int](_ max _))
      val zip2 = b.add(ZipWith[Int, Int, Int](_ max _))
      zip1.out ~> zip2.in0
      UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
    }
    val resultSink = Sink.head[Int]
    val g = RunnableGraph.fromGraph((GraphDSL.create(resultSink) { implicit b => sinkS =>
      import GraphDSL.Implicits._
      val max = b.add(pickMax)
      Source.single(1) ~> max.in(0)
      Source.single(2) ~> max.in(1)
      Source.single(3) ~> max.in(2)
      max.out ~> sinkS.in
      ClosedShape
    }))
    g.run.foreach(println)
  }
  def test4 = {
    val pairs = Source.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val zip = b.add(Zip[Int, Int])
      def ints = Source.fromIterator(() => Iterator.from(1))
      ints.filter(_ % 2 != 0) ~> zip.in0
      ints.filter(_ % 2 == 0) ~> zip.in1
      SourceShape(zip.out)
    })
    pairs.runWith(Sink.head).foreach(println)
  }
  def test5 = {
    val pairs = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val broadcast = b.add(Broadcast[Int](2))
      val zip = b.add(Zip[Int, String])
      broadcast.out(0) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1
      FlowShape(broadcast.in, zip.out)
    })
    pairs.runWith(Source.single(1), Sink.head)._2.foreach(println)
  }
  def test6 = {
    case class PriorityWorkerPoolShape[In, Out](
        jobsIn: Inlet[In],
        priorityJobsIn: Inlet[In],
        resultsOut: Outlet[Out]) extends Shape {
      override val inlets: immutable.Seq[Inlet[_]] = jobsIn :: priorityJobsIn :: Nil
      override val outlets: immutable.Seq[Outlet[_]] = resultsOut :: Nil
      override def deepCopy = PriorityWorkerPoolShape(jobsIn.carbonCopy, priorityJobsIn.carbonCopy, resultsOut.carbonCopy)
    }
    class PriorityWorkerPoolShape2[In, Out](init: Init[Out] = Name("PriorityWorkerPoolShape2")) extends FanInShape[Out](init) {
      protected override def construct(init: Init[Out]) = new PriorityWorkerPoolShape2(init)
      val jobsIn = newInlet[In]("jobsIn")
      val priorityJobsIn = newInlet[In]("priorityJobsIn")
    }
    object PriorityWorkerPool {
      def apply[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Graph[PriorityWorkerPoolShape[In, Out], NotUsed] = {
        GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._
          val priorityMerge = b.add(MergePreferred[In](1))
          val balance = b.add(Balance[In](workerCount))
          val merge = b.add(Merge[Out](workerCount))
          priorityMerge.out ~> balance.in
          for (i <- 0 until workerCount) balance.out(i) ~> worker ~> merge.in(i)
          PriorityWorkerPoolShape(priorityMerge.in(0), priorityMerge.preferred, merge.out)
        }
      }
    }
    val worker1 = Flow[String].map("step 1 " + _)
    val worker2 = Flow[String].map("step 2 " + _)
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val priorityPool1 = b.add(PriorityWorkerPool(worker1, 4))
      val priorityPool2 = b.add(PriorityWorkerPool(worker2, 2))
      Source(1 to 100).map("job: " + _) ~> priorityPool1.jobsIn
      Source(1 to 100).map("priority job: " + _) ~> priorityPool1.priorityJobsIn
      priorityPool1.resultsOut ~> priorityPool2.jobsIn
      Source(1 to 100).map("on-step, priority " + _) ~> priorityPool2.priorityJobsIn
      priorityPool2.resultsOut ~> Sink.foreach(println)
      ClosedShape
    }).run
  }
//  def test7 = {
//    val foldSink = Sink.fold[Int, Int](0)(_ + _)
//    Source(0 to 9).runWith(foldSink).foreach(println)
//    val foldFlow: Flow[Int, Int, Future[Int]] = Flow.fromGraph(GraphDSL.create(foldSink) { implicit b => fold =>
//      FlowShape(fold.in, b.materializedValue.map)
//    })
//    Source(0 to 9).via(foldFlow).runForeach(println)
//  }
  test6
  Thread.sleep(3000)
  system.terminate
}
