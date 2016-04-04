package com.sebastianvoss.template.api

package object domain {

  import reactivemongo.bson.{BSONDocumentWriter, BSONDocument, BSONDocumentReader}
  import spray.json.DefaultJsonProtocol._

  implicit val userFormat = jsonFormat2(User)

  implicit object UserReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument): User = {
      User(
        doc.getAs[Long]("_id").get,
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
