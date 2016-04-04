package com.sebastianvoss.template.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.sebastianvoss.template.api.domain.{User, _}
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import spray.json.DefaultJsonProtocol._
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

object ReactiveRestApi extends App {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val driver = new MongoDriver
  val connection = driver.connection(List("192.168.99.100"))
  val db = connection("sample")
  val users: BSONCollection = db("users")
  val userRepository = UserRepository(users)

  val route: Route =
    get {
      pathPrefix("user" / LongNumber) { id =>
        val maybeUser: Future[Option[User]] = userRepository.fetchUser(id)
        onSuccess(maybeUser) {
          case Some(user) => complete(user)
          case None => complete(StatusCodes.NotFound)
        }
      }
    } ~
      get {
        pathPrefix("user") {
          val users: Future[List[User]] = userRepository.fetchUsers()
          onSuccess(users) {
            case users => complete(users)
          }
        }
      } ~
      post {
        pathPrefix("user") {
          entity(as[User]) { user =>
            val f = userRepository.saveUser(user)
            onComplete(f) {
              case Failure(e) => complete {
                StatusCodes.BadRequest -> Map("error" -> e.getMessage)
              }
              case Success(value) => complete {
                StatusCodes.Created -> user
              }
            }
          }
        }
      }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ {
    system.terminate()
    driver.close()
  })
}
