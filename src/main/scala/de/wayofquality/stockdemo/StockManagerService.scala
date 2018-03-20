package de.wayofquality.stockdemo

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask
import de.wayofquality.stockdemo.StockManager.{CreateArticle, Stock}

class StockManagerService(stockManager: ActorRef) extends StockManagerJsonSupport {

  import StockManager.ListArticles

  implicit val askTimeout = Timeout(1.second)

  val route : Route = path("articles") {
    get {
      onSuccess(stockManager ? ListArticles) {
        case stock : Stock => complete(stock)
      }
    } ~
    post {
      entity(as[Article]) { article =>
        onSuccess(stockManager ? CreateArticle(article)) {
          case result : StockManager.StockManagerResult => complete(result)
        }
      }
    }
  }

}