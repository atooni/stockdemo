package de.wayofquality.stockdemo

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.{Marshal, ToResponseMarshallable}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import de.wayofquality.stockdemo.StockManager._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class StockManagerService(stockManager: ActorRef) extends StockManagerJsonSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val askTimeout = Timeout(1.second)

  // A convenience method to turn the response from the StockManager into a
  // HTTP Response. A Failure will result in a "Internal Server error",
  // if the request couldn't be procesed, the associated StockManagerResult
  // will be returned with Status "BadRequest"
  // A normal result will yield in Status "OK"
  private def response(rc: Try[Any]) : Future[HttpResponse] = {
    rc match {
      case Success(result) if result.isInstanceOf[StockManagerResult] =>
        val mgrResult = result.asInstanceOf[StockManagerResult]
        if (mgrResult.rc == 0) {
          Marshal(mgrResult).to[HttpResponse]
        } else {
          Marshal(mgrResult).to[HttpResponse].map {
            resp => resp.copy(status = StatusCodes.BadRequest)
          }
        }
      case Success(result) if result.isInstanceOf[Stock] =>
        Marshal(result.asInstanceOf[Stock]).to[HttpResponse]
      case Success(r) =>
        Marshal(StatusCodes.InternalServerError -> s"Unexpected result of type [${r.getClass().getName()}]").to[HttpResponse]
      case Failure(e) =>
        Marshal(StatusCodes.BadRequest -> e.getMessage()).to[HttpResponse]
    }
  }

  def executeStockManager(msg: Any) : Route =
    onComplete((stockManager ? msg)) { result =>
      complete(response(result))
    }

  private val articleRoute : Route = path("articles") {
    get {
      executeStockManager(ListArticles)
    } ~
      post {
        entity(as[Article]) { article =>
          executeStockManager(CreateArticle(article))
        }
      }
  } ~
  pathPrefix("articles" / LongNumber) { articleId =>
    path("refill") {
      entity(as[Quantity]) { q =>
        executeStockManager(Refill(articleId, q.quantity))
      }
    }
  }

  val route = articleRoute

}