package de.wayofquality.stockdemo


import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.wayofquality.stockdemo.StockManager._
import org.scalatest.{FreeSpec, Matchers}
import de.wayofquality.stockdemo.StockManagerJsonSupport._

import scala.util.Try

class RestServiceSpec extends FreeSpec
  with Matchers
  with ScalatestRouteTest {

  val product = Article(1, "Super Computer", 100)

  private[this] def withRestService(f: Route => Unit) : Unit = {
    val stockManager = system.actorOf(props())
    val route = new StockManagerService(stockManager).route

    f(route)

    system.stop(stockManager)
  }

  private[this] def getProducts(route: Route) : Try[Stock] = Try {

    Get("/articles") ~> route ~> check {
      responseAs[Stock]
    }
  }

  // convenenience method to check the result of the current stock state
  // not taking reservations into account
  def checkArticles(
    articleCount : Long,
    contained : List[Article],
    stock: Stock
  ) : List[ArticleState] = {
    val l = stock.products.map(_.article)

    assert(
      l.length == articleCount &&
        contained.forall { a => l.contains(a) }
    )

    stock.products
  }

  "The Stock Manager Service should " - {

    "Allow to create a product along with an available Quantity" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult].rc == 0)
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Deny to create a product with an id that already exists" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post("/articles", product) ~> route ~> check {
          assert(status == StatusCodes.BadRequest)
          assert(responseAs[StockManager.StockManagerResult] === ARTICLE_ALREADY_EXISTS.copy(article = Some(product)))
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Allow to increase the available quantity of an existing product" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/refill", Quantity(5)) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        checkArticles(1, List(product.copy(onStock = 105)), getProducts(route).get)
      }
    }

    "Deny to increase the available quantity of a non-existing product" in {
      pending
    }

    "Allow to sell an existing product if sufficiently available" in {
      pending
    }

    "Deny to sell a non-existing product" in {
      pending
    }

    "Deny to buy an existing product if not sufficiently available" in {
      pending
    }

    "Allow to reserve a product if sufficiently available" in {
      pending
    }

    "Deny to reserve a non-existing product" in {
      pending
    }

    "Deny to reserve a product if not sufficiently available" in {
      pending
    }

    "Allow to cancel a reservation and make the quantity reserved available again" in {
      pending
    }

    "Take into account the current reservations when checking the current stock" in {
      pending
    }
  }
}
