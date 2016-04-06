package com.sebastianvoss.template.api

import java.util.UUID
import akka.Done
import com.sebastianvoss.template.api.domain.User
import reactivemongo.api.MongoConnection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONDocument
import com.sebastianvoss.template.api.domain.{User, _}
import reactivemongo.core.errors.DatabaseException
import scala.concurrent.{Promise, ExecutionContext, Future}

trait UserRepository {
  def getAll(): Future[List[User]]

  def getById(id: String): Future[Option[User]]

  def create(user: User): Future[User]

  def update(id: String, user: User): Future[Done]

  def delete(id: String): Future[Done]
}

class MemoryUserRepository(implicit ec: ExecutionContext) extends UserRepository {
  private var users = scala.collection.mutable.Map[String, User]()

  override def getAll(): Future[List[User]] = Future {
    users.values.toList
  }

  override def update(id: String, user: User): Future[Done] = Future {
    val oldUser = users(id)
    val newUser = oldUser.copy(name = user.name)
    users(id) = newUser
    Done
  }

  override def delete(id: String): Future[Done] = Future {
    users -= id
    Done
  }

  override def getById(id: String): Future[Option[User]] = Future {
    if (users.isDefinedAt(id)) Some(users(id)) else None
  }

  override def create(user: User): Future[User] = Future {
    val uuid = UUID.randomUUID().toString
    val u = user.copy(id = Some(uuid))
    users(uuid) = u
    u
  }
}

class MongoUserRepository(connection: MongoConnection)(implicit ec: ExecutionContext) extends UserRepository {
  val db = connection.db("sample")
  val users = db.collection[BSONCollection]("users")

  def getAll(): Future[List[User]] = {
    val query = BSONDocument()
    users.
      find(query).
      cursor[User]().
      collect[List]()
  }

  def getById(id: String) = {
    println("fetching user...")
    val query = BSONDocument("_id" -> id)
    users.find(query).one[User]
  }

  def create(user: User): Future[User] = {
    val u = user.copy(id = Some(UUID.randomUUID().toString))
    val future = users.insert(u)
    future.map(writeResult => u)
  }

  def update(id: String, user: User): Future[Done] = {
    println(s"Updating user $id")
    val future = users.update(BSONDocument("_id" -> id), user, upsert = false, multi = false)
    future.map {
      case r: WriteResult if r.n > 0 => Done
      case r: WriteResult if r.n == 0 => throw new EntityNotFoundException(s"User $id does not exist")
      case _ => throw new Exception("error")
    }
  }

  def delete(id: String): Future[Done] = {
    val query = BSONDocument("_id" -> id)
    users.remove(query).map {
      case r: WriteResult if r.n > 0 => Done
      case r: WriteResult if r.n == 0 => throw new Exception(s"User $id does not exist")
      case _ => throw new Exception("error")
    }
  }
}

object MongoUserRepository {
  def apply(connection: MongoConnection)(implicit ec: ExecutionContext) = new MongoUserRepository(connection)
}
