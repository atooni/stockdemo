package de.wayofquality.stockdemo

// A case class representing a product.
// The constructor is private to enforce creation via the apply method.
final case class Article(
  id: Long,
  name: String,
  onStock: Long
)

// A case class representing the state of an Article including pending reservations.
final case class ArticleState(
  article: Article,
  reservations: List[Reservation]
) {

  def available : Long = article.onStock - reservations.map(_.reservedQuantity).sum
}


