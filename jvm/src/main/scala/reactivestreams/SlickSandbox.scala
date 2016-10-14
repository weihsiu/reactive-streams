package reactivestreams.database

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global

object SlickSandbox extends App {
  case class User(id: Int, name: String)
  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def * = (id, name) <> (User.tupled, User.unapply)
  }
  val users = TableQuery[Users]
  val setup = DBIO.seq(
    users.schema.create,
    users ++= Seq(User(1, "Walter"), User(2, "Adrian"), User(3, "Brian"))
  )
  val db = Database.forConfig("h2")
  try {
    val r1 = for {
      _ <- db.run(setup)
      us <- db.run(users.result)
    } yield us
    println(Await.result(r1, 3.seconds))
    db.run(users += User(4, "Margarit")).foreach(r => println(s"insert result: $r"))
    val r2 = db.stream(users.map(_.name).result).foreach(println)
    StdIn.readLine
  } finally db.close
}
