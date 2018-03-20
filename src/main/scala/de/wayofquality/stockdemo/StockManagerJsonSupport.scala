package de.wayofquality.stockdemo

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.wayofquality.stockdemo.StockManager.Stock
import spray.json.DefaultJsonProtocol

trait StockManagerJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val cancelFormat = jsonFormat1(CancelReservation)
  implicit val reservationFormat = jsonFormat4(Reservation)
  implicit val articleFormat = jsonFormat3(Article)
  implicit val articleStateFormat = jsonFormat2(ArticleState)
  implicit val stockFormat = jsonFormat1(Stock)
  implicit val resultFormat = jsonFormat3(StockManager.StockManagerResult)
}
