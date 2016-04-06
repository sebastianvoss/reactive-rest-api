package com.sebastianvoss.template.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.{Materializer, ActorMaterializer}
import com.sebastianvoss.template.api.domain.{User, _}
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import spray.json.DefaultJsonProtocol._
import scala.concurrent.{ExecutionContextExecutor, ExecutionContext, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

trait Service {
  implicit val system: ActorSystem
  implicit val materializer: Materializer
  implicit val userRepository: UserRepository

  implicit def executor: ExecutionContextExecutor

  val route: Route =
    pathPrefix("user") {
      (get & path(Segment)) { id =>
        val f = userRepository.getById(id)
        onSuccess(f) {
          case Some(user) => complete(user)
          case None => complete(StatusCodes.NotFound -> s"User $id not found")
        }
      } ~
        get {
          complete {
            userRepository.getAll()
          }
        } ~
        (post & entity(as[User])) { user =>
          extractUri { uri =>
            val f = userRepository.create(user)
            onComplete(f) {
              case Success(u) => {
                respondWithHeaders(Location(uri.withPath(uri.path / u.id.get.toString))) {
                  complete(StatusCodes.Created -> u)
                }
              }
              case Failure(e) => complete(StatusCodes.BadRequest -> e.getMessage)
            }
          }
        } ~
        (put & entity(as[User])) { user =>
          path(Segment) { id =>
            val f = userRepository.update(id, user)
            onComplete(f) {
              case Success(_) => {
                complete(StatusCodes.NoContent)
              }
              case Failure(e: EntityNotFoundException) => complete(StatusCodes.NotFound -> e.getMessage)
              case Failure(e) => complete(StatusCodes.InternalServerError -> e.getMessage)
            }
          }
        } ~
        (delete & path(Segment)) { id =>
          val f = userRepository.delete(id)
          onComplete(f) {
            case Success(u) => {
              complete(StatusCodes.NoContent)
            }
            case Failure(e) => complete(StatusCodes.BadRequest -> e.getMessage)
          }
        }
    }
}

object ReactiveRestApi extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  val driver = new MongoDriver
  val connection = driver.connection(List("192.168.99.100"))
  val userRepository = MongoUserRepository(connection)

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ {
    driver.close()
    system.terminate()
  })
}
