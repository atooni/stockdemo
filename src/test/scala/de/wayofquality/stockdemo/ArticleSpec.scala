package de.wayofquality.stockdemo

import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._

class ArticleSpec extends FreeSpec with Matchers {

  val foo = Article(1, "Foo", 100)
  val article = Article(2, "Super Computer", 10)

  "An article" - {

    "should always have a quantity >= 0" in {
      assert(foo.name === "Foo")
      assert(foo.onStock === 100)
    }
  }

  "An article state" - {

    "with no reservations should have the entire stock available" in {
      val state = ArticleState(article, List.empty)
      assert(state.available === article.onStock)
    }

    "should reflect the pending reservations in it's availablity" in {
      val state = ArticleState(article, List(
        Reservation(1, article.id, 4, 1.minute.toMillis),
        Reservation(2, article.id, 1, 1.minute.toMillis)
      ))
      assert(state.available === article.onStock - 4 - 1)

    }
  }
}
