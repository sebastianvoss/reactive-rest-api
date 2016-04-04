package com.sebastianvoss.template.api

import com.sebastianvoss.template.api.domain.User
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import com.sebastianvoss.template.api.domain.{User, _}

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(users: BSONCollection) {
  implicit val ec = ExecutionContext.global

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
    future.map(_ => user)
  }
}

object UserRepository {
  def apply(users: BSONCollection): UserRepository = new UserRepository(users)
}
