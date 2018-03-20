package de.wayofquality.stockdemo

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.FiniteDuration

object Reservation {

  private[this] val reservationCounter = new AtomicLong(0)

  def apply(
    // The id of the product te be reserved
    articleId: Long,
    // The amount to be reserved
    reservedQuantity: Long,
    // The duration for the reservation
    reservedFor: FiniteDuration
  ): Reservation = Reservation(
    id = reservationCounter.incrementAndGet(),
    articleId,
    reservedQuantity,
    reservedFor
  )
}

// Reserving a product with a given Quantity and a certain period
case class Reservation private (
  // The unique identifier of a reservation, for now this is also a Long
  id: Long,
  // The id of the product te be reserved
  articleId : Long,
  // The amount to be reserved
  reservedQuantity: Long,
  // The duration for the reservation
  reservedFor: FiniteDuration
) {

}

// Signalling the timeout of a Reservation
case class CancelReservation(id: Long)
