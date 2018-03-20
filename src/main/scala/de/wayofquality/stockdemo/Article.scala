package de.wayofquality.stockdemo

import java.util.concurrent.atomic.AtomicLong

import de.wayofquality.stockdemo.StockManager.Reservation

// Companion class for Products.
// Maintains a simple counter for new product id's and provides
// a constructor to validate product quantity.
object Article {

  private[this] val idCounter = new AtomicLong(0)

  def apply(
    name: String,
    onStock: Long
  ): Article = {
    require(onStock >= 0)
    Article(idCounter.incrementAndGet(), name, onStock)
  }
}

// A case class representing a product.
// The constructor is private to enforce creation via the apply method.
case class Article private(
  id: Long,
  name: String,
  onStock: Long
)

// A case class representing the state of an Article including pending reservations.
case class ArticleState(
  article: Article,
  reservations: List[Reservation]
) {

  def available : Long = article.onStock - reservations.map(_.reservedQuantity).sum
}
