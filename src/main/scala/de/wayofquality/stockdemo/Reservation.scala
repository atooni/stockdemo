package de.wayofquality.stockdemo

// Reserving a product with a given Quantity and a certain period
final case class Reservation (
  // The unique identifier of a reservation, for now this is also a Long
  id: Long,
  // The id of the product te be reserved
  articleId : Long,
  // The amount to be reserved
  reservedQuantity: Long,
  // The duration for the reservation in milliseconds
  reservedFor: Long
)

// Signalling the cancellation of a Reservation
case class CancelReservation(id: Long)

// Signalling the fulfillment of a reservation
case class FulfillReservation(id: Long)
