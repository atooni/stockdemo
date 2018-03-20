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

  private[this] var currentStock : List[Article] = List.empty

  override def receive: Receive = {
    // Creating an article that does not yet exist in the stock
    case CreateArticle(a) =>
      log.debug(s"Adding article [$a] to current stock")
      currentStock = a :: currentStock
      sender() ! StockManagerResult(0)

    // List the current stock
    case ListArticles =>
      log.debug(s"Return [${currentStock.length}] articles in stock.")
      sender() ! Stock(currentStock)
  }
}
