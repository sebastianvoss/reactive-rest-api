package com.sebastianvoss.template.api

import com.sebastianvoss.template.api.domain.User
import reactivemongo.api.MongoConnection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import com.sebastianvoss.template.api.domain.{User, _}

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(connection: MongoConnection)(implicit ec: ExecutionContext) {
  val db = connection.db("sample")
  val users = db.collection[BSONCollection]("users")

  def fetchUsers(): Future[List[User]] = {
    val query = BSONDocument()
    users.
      find(query).
      cursor[User]().
      collect[List]()
  }

  def fetchUser(id: Long) = {
    val query = BSONDocument("_id" -> id)
    users.find(query).one[User]
  }

  def saveUser(user: User): Future[User] = {
    val future = users.insert(user)
    future.flatMap {
      case writeResult if (writeResult.code contains 11000) => Future(user)
      case _ => Future.failed(new Exception("DB error"))
    }
  }
}

object UserRepository {
  def apply(connection: MongoConnection)(implicit ec: ExecutionContext) = new UserRepository(connection)
}
