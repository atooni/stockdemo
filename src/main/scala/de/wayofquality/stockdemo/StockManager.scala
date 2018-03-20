package de.wayofquality.stockdemo

import akka.actor.{Actor, ActorLogging, Props}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

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
  val ARTICLE_NOT_FOUND = StockManagerResult(2, None, Option("Article does not exist"))
  val ARTICLE_UNSUFFICIENT_STOCK = StockManagerResult(2, None, Option("Unsufficient stock"))
  val RESERVATION_NOT_FOUND  = StockManagerResult(3, None, Option("Reservation not found"))

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

  private[this] implicit val eCtxt = context.system.dispatcher

  // Maintain the current stock as a Map. The Map is immutable for now, so this
  // should be optimised implementing a real world use case. But then of course
  // it wouldn't reside in memory
  private[this] var currentStock : Map[Long, Article] = Map.empty

  // This is the list of current reservations, also keyed by the article id
  private[this] var currentReservations : Map[Long, List[Reservation]] = Map.empty

  // convenience method to retrieve an article with it's id. When the implementation
  // of the stock store changes, we just need to change this method
  private[this] def article(id: Long) : Option[Article] = currentStock.get(id)

  // convenience method to retrieve the reservations for an article
  private[this] def reservations(id: Long) : List[Reservation] = currentReservations.getOrElse(id, List.empty)

  // convenience method to calculate the freely available quantity of a given
  // product taken the stock and reservations into account
  private[this] def available(id: Long) : Try[(Article, Long)] = Try {
    article(id) match {
      case Some(a) =>
        (a, a.onStock - reservations(a.id).map(_.reservedQuantity).sum)
      case None =>
        throw new Exception("Article does not exist")
    }
  }

  // convenience method to save an article
  private[this] def saveArticle(a: Article): Unit =
    currentStock = currentStock.filterKeys(_ != a.id) + (a.id -> a)

  // convenience method to save a reservation
  private[this] def saveReservation(reservation: Reservation) : Unit = {
    // When saving the reservation we will turn the duration into an absoulute time

    val articleReservations : List[Reservation] =
      reservation.copy(
        reservedFor = System.currentTimeMillis() + reservation.reservedFor
      ) :: currentReservations.getOrElse(reservation.articleId, List.empty)

    // Now we schedule a Timeout Message
    context.system.scheduler.scheduleOnce(reservation.reservedFor.millis, self, TimeoutReservation(reservation.id))

    // And update the reservation store
    currentReservations =
      currentReservations.filterKeys(_ != reservation.articleId) + (reservation.articleId -> articleReservations)
  }

  // convenience method to find a reservation by it's id
  private[this] def reservationById(id: Long) : Option[Reservation] =
    currentReservations.values.flatten.find(_.id == id)

  // convenience method to remove a reservation
  private[this] def removeReservation(r: Reservation) : Unit = {
    val newReservations = reservations(r.articleId).filter(_.id != r.id)
    currentReservations = currentReservations.filterKeys(_ != r.articleId) + (r.articleId -> newReservations)
  }

  // handle all protocol messages managing the stock
  override def receive: Receive = {
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
          sender() ! ARTICLE_NOT_FOUND
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
          sender() ! ARTICLE_NOT_FOUND
      }

    case r : Reservation =>
      available(r.articleId) match {
        case Success((a,q)) =>
          if (q < r.reservedQuantity) {
            log.debug(s"Insufficient stock for [$a], requested: [${r.reservedQuantity}]")
            sender() ! ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(a))
          } else {
            log.debug(s"Reserving [${r.reservedQuantity}] of [$a]")
            saveReservation(r)
            sender() ! StockManagerResult(0)
          }
        case Failure(_) =>
          log.debug(s"Article with id [${r.articleId}] not found")
          sender() ! ARTICLE_NOT_FOUND
      }

    case CancelReservation(id) =>
      reservationById(id) match {
        case Some(r) =>
          log.debug(s"Removing reservation [$r]")
          removeReservation(r)
          sender() ! StockManagerResult(0)
        case None =>
          log.debug(s"Reservation [$id] not found.")
          sender() ! RESERVATION_NOT_FOUND
      }

    case FulfillReservation(id) =>
      reservationById(id) match {
        case Some(r) =>
          log.debug(s"Fulfilling reservation [$r]")
          removeReservation(r)
          article(r.articleId) match {
            case Some(a) =>
              saveArticle(a.copy(onStock = a.onStock - r.reservedQuantity))
              sender() ! StockManagerResult(0)
            case None =>
              sender() ! ARTICLE_NOT_FOUND
          }
        case None =>
          log.debug(s"Reservation [$id] not found.")
          sender() ! RESERVATION_NOT_FOUND
      }

    case TimeoutReservation(id) =>
      reservationById(id) match {
        case Some(r) =>
          log.debug(s"Reservation [$id] timed out.")
          removeReservation(r)
        case None => // do nothing
      }

    // List the current stock
    case ListArticles =>
      log.debug(s"Return [${currentStock.size}] articles in stock.")
      sender() ! Stock(currentStock.values.toList.map(a => ArticleState(a, currentReservations.getOrElse(a.id, List.empty))))
  }
}
