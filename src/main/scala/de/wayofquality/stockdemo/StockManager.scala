package de.wayofquality.stockdemo

import akka.actor.{Actor, ActorLogging}

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

  override def receive: Receive = Actor.emptyBehavior
}
