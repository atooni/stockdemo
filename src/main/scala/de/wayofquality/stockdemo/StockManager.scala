package de.wayofquality.stockdemo

import akka.actor.{Actor, ActorLogging, Props}

// The companion object for the StockManager defines the protocol
// messages for the various use cases
object StockManager {

  // Adding a new product to the StockManager
  case class CreateArticle(p: Article)

  // Asking for all products and the corresponding product list
  case object ListArticles
  case class Stock(products: List[Article])

  // A general result message indicating success (rc = 0) or an error
  // code along with an error message
  case class StockManagerResult(
    rc: Int,
    reason: Option[String] = None
  )

  val ARTICLE_ALREADY_EXISTS = StockManagerResult(1, Option("Product already exists"))

  // Return the props object to create the main Actor
  def props() : Props = Props(new StockManager)
}
/**
  * The StockManager is the backend to the super simple stock manager REST API.
  * It will manage a palette of products, associated quantity on Stock and product
  * reservations.
  *
  * For demo purposes all of this is done in memory only and will not be persisted
  * across restarts.
  *
  * This backend will be used from the REST interface defined via Akka HTTP.
  */
class StockManager extends Actor with ActorLogging {

  import StockManager._

  private[this] var currentStock : Map[Long, Article] = Map.empty

  override def receive: Receive = {
    // Creating an article that does not yet exist in the stock
    case CreateArticle(a) =>
      currentStock.get(a.id) match {
        case Some(_) =>
          log.debug(s"Article with id [${a.id}] already exists.")
          sender() ! ARTICLE_ALREADY_EXISTS
        case None =>
          log.debug(s"Adding article [$a] to current stock")
          currentStock = currentStock + (a.id -> a)
          sender() ! StockManagerResult(0)
      }


    // List the current stock
    case ListArticles =>
      log.debug(s"Return [${currentStock.size}] articles in stock.")
      sender() ! Stock(currentStock.values.toList)
  }
}
