package reactivestreams.database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.driver.H2Driver.api._

object H2Database {
  case class User(id: Option[Int], name: String)
  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def * = (id.?, name) <> (User.tupled, User.unapply)
  }
  val users = TableQuery[Users]
  val database = {
    val db = Database.forConfig("h2")
    db.run(
      DBIO.seq(
        users.schema.create,
        users ++= Seq(User(None, "Walter"), User(None, "Adrian"), User(None, "Brian"))
      )
    ).map(Function.const(db))
  }
}
