package de.wayofquality.stockdemo

import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._

class ArticleSpec extends FreeSpec with Matchers {

  "An article" - {

    "should always have a quantity >= 0" in {

      val p = Article("Foo", 100)

      assert(p.name === "Foo")
      assert(p.onStock === 100)

      an [Exception] should be thrownBy ( Article("Bar", -3))
    }
  }

  "An article state" - {

    "with no reservations should have the entire stock available" in {

      val article = Article("Super Computer", 10)
      val state = ArticleState(article, List.empty)
      assert(state.available === article.onStock)
    }

    "should reflect the pending reservations in it's availablity" in {
      val article = Article("Super Computer", 10)
      val state = ArticleState(article, List(
        Reservation(article.id, 4, 1.minute),
        Reservation(article.id, 1, 1.minute)
      ))
      assert(state.available === article.onStock - 4 - 1)

    }
  }
}
