package de.wayofquality.stockdemo

import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

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
    soldQuantity: Long
  )

  // Reserving a product with a given Quantity and a certain period
  case class Reservation(
    // The id of the product te be reserved
    id : Long,
    // The amount to be reserved
    reservedQuantity: Long,
    // The duration for the reservation
    reservedFor: FiniteDuration
  )

  // Signalling the timeout of a Reservation
  case class CancelReservation(
    reservation: Reservation
  )

  // Asking for all products and the corresponding product list
  case object ListArticles
  case class Stock(products: List[ArticleState])

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

  // Maintain the current stock as a Map. The Map is immutable for now, so this
  // should be optimised implementing a real world use case. But then of course
  // it wouldn't reside in memory
  private[this] var currentStock : Map[Long, Article] = Map.empty

  // convenience method to retrieve an article with it's id. When the implementation
  // of the stock store changes, we just need to change this method
  private[this] def article(id: Long) : Option[Article] = currentStock.get(id)

  // convenience method to calculate the freely available quantity of a given
  // product taken the stock and reservations into account
  private[this] def available(id: Long) : Try[(Article, Long)] = Try {
    article(id) match {
      case Some(a) =>
        (a, a.onStock)
      case None =>
        throw new Exception("Article does not exist")
    }
  }

  private[this] def saveArticle(a: Article): Unit = currentStock = currentStock.filterKeys(_ != a.id) + (a.id -> a)

  override def receive: Receive = {
    // Creating an article that does not yet exist in the stock
    case CreateArticle(a) =>
      article(a.id) match {
        case Some(_) =>
          log.debug(s"Article with id [${a.id}] already exists.")
          sender() ! ARTICLE_ALREADY_EXISTS.copy(article = Some(a))
        case None =>
          log.debug(s"Adding article [$a] to current stock")
          currentStock = currentStock + (a.id -> a)
          sender() ! StockManagerResult(0)
      }

    case Refill(id, quantity) =>
      article(id) match {
        case Some(a) =>
          log.debug(s"Adding [$quantity] of {$a} to current stock.")
          saveArticle(a.copy(onStock = a.onStock + quantity))
          sender() ! StockManagerResult(0)
        case None =>
          log.debug("Article with id [$id] not found")
          sender() ! ARTICLE_DOES_NOT_EXIST
      }

    case Sell(id, quantity) =>
      available(id) match {
        case Success((a,q)) =>
          if (q < quantity) {
            log.debug(s"Insufficient stock for [$a], requested: [$quantity]")
            sender() ! ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(a))
          } else {
            log.debug(s"Selling [$quantity] of [$a]")
            saveArticle(a.copy(onStock = a.onStock - quantity))
            sender() ! StockManagerResult(0)
          }
        case Failure(_) =>
          log.debug(s"Article with id [$id] not found")
          sender() ! ARTICLE_DOES_NOT_EXIST
      }

    case Reservation(id, quantity, reservedFor) =>
      article(id) match {
        case Some(a) =>
        case None =>
          log.debug("Article with id [$id] not found")
          sender() ! ARTICLE_DOES_NOT_EXIST
      }

    // List the current stock
    case ListArticles =>
      log.debug(s"Return [${currentStock.size}] articles in stock.")
      sender() ! Stock(currentStock.values.toList.map(a => ArticleState(a, List.empty)))
  }
}
