package de.wayofquality.stockdemo

import akka.actor.{Actor, ActorLogging, Props}
import de.wayofquality.stockdemo.StockManager.CreateProduct

// The companion object for the StockManager defines the protocol
// messages for the various use cases
object StockManager {

  // Adding a new product to the StockManager
  case class CreateProduct(p: Product)

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

  override def receive: Receive = {
    case CreateProduct(_) => sender() ! StockManagerResult(0)
  }
}
