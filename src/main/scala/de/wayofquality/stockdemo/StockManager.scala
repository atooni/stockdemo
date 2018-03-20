package de.wayofquality.stockdemo

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Try

// The companion object for the StockManager defines the protocol
// messages for the various use cases
object StockManager {

  // Adding a new product to the StockManager
  case class CreateArticle(p: Article)

  // Refilling a product with a given Quantity
  case class Refill(
    // The id of the target product
    id: Long,
    // The amount to be added to the stock
    addedAmount: Long
  )

  // Selling a product with a given Quantity
  case class Sell(
    // The id of the target product
    id: Long,
    // The amount sold
    soldAmount: Long
  )

  // Asking for all products and the corresponding product list
  case object ListArticles
  case class Stock(products: List[Article])

  // A general result message indicating success (rc = 0) or an error
  // code along with an error message
  case class StockManagerResult(
    rc: Int,
    article: Option[Article] = None,
    reason: Option[String] = None
  )

  val ARTICLE_ALREADY_EXISTS = StockManagerResult(1, None, Option("Article already exists"))
  val ARTICLE_DOES_NOT_EXIST = StockManagerResult(2, None, Option("Article does not exist"))
  val ARTICLE_UNSUFFICIENT_STOCK = StockManagerResult(2, None, Option("Unsufficient stock"))

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

  private def saveArticle(a: Article): Unit = currentStock = currentStock.filterKeys(_ != a.id) + (a.id -> a)

  override def receive: Receive = {
    // Creating an article that does not yet exist in the stock
    case CreateArticle(a) =>
      currentStock.get(a.id) match {
        case Some(_) =>
          log.debug(s"Article with id [${a.id}] already exists.")
          sender() ! ARTICLE_ALREADY_EXISTS.copy(article = Some(a))
        case None =>
          log.debug(s"Adding article [$a] to current stock")
          currentStock = currentStock + (a.id -> a)
          sender() ! StockManagerResult(0)
      }

    case Refill(id, quantity) =>
      currentStock.get(id) match {
        case Some(a) =>
          log.debug(s"Adding [$quantity] of {$a} to current stock.")
          saveArticle(a.copy(onStock = a.onStock + quantity))
          sender() ! StockManagerResult(0)
        case None =>
          log.debug("Article with id [$id] not found")
          sender() ! ARTICLE_DOES_NOT_EXIST
      }

    case Sell(id, quantity) => {
      currentStock.get(id) match {
        case Some(a) =>
          if (a.onStock < quantity) {
            log.debug(s"Insufficient stock for [$a], requested: [$quantity]")
            sender() ! ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(a))
          } else {
            log.debug(s"Selling [$quantity] of [$a]")
            saveArticle(a.copy(onStock = a.onStock - quantity))
            sender() ! StockManagerResult(0)
          }
        case None =>
          log.debug("Article with id [$id] not found")
          sender() ! ARTICLE_DOES_NOT_EXIST
      }
    }

    // List the current stock
    case ListArticles =>
      log.debug(s"Return [${currentStock.size}] articles in stock.")
      sender() ! Stock(currentStock.values.toList)
  }
}
