import akka.actor.ActorSystem
import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{StatusCodes, HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.sebastianvoss.template.api.domain.User
import com.sebastianvoss.template.api.{MemoryUserRepository, MongoUserRepository, Service, UserRepository}
import org.scalatest._
import reactivemongo.api.MongoDriver
import scala.concurrent.duration._

import scala.concurrent.{Await, Future, ExecutionContextExecutor}

class TestService(sys: ActorSystem, e: ExecutionContextExecutor, m: ActorMaterializer, ur: UserRepository) extends Service {
  val materializer = m
  val userRepository = ur
  val system = sys

  def actorRefFactory = sys

  def executor = e
}

class FullTestKitExampleSpec extends WordSpec with Matchers with ScalatestRouteTest {
  val userRepository = new MemoryUserRepository()
  val f = userRepository.create(User(None, "Test"))
  val user = Await.result(f, 5 seconds)

  val service = new TestService(system, system.dispatcher, ActorMaterializer(), userRepository)

  "The User API" should {

    "return an existing user" in {
      Get(s"/user/${user.id.get}") ~> service.route ~> check {
        responseAs[User] shouldBe user
      }
    }

    "return 404 for non-existing user" in {
      Get(s"/user/123") ~> service.route ~> check {
        response.status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
