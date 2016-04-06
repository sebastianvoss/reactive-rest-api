package com.sebastianvoss.template.api

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromResponseUnmarshaller}
import akka.stream.Materializer

import scala.concurrent.{Future, ExecutionContext}

package object domain {

  import reactivemongo.bson.{BSONDocumentWriter, BSONDocument, BSONDocumentReader}
  import spray.json.DefaultJsonProtocol._

  implicit val userFormat = jsonFormat2(User)

  implicit object UserReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument): User = {
      User(
        doc.getAs[String]("_id"),
        doc.getAs[String]("name").get
      )
    }
  }

  implicit object UserWriter extends BSONDocumentWriter[User] {
    def write(user: User): BSONDocument =
      BSONDocument(
        "_id" -> user.id,
        "name" -> user.name
      )
  }

}
