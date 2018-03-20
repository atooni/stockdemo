package de.wayofquality.stockdemo

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.wayofquality.stockdemo.StockManager._
import de.wayofquality.stockdemo.StockManagerJsonSupport._
import org.scalatest.{FreeSpec, Matchers}
import scala.concurrent.duration._
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
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/2/refill", Quantity(5)) ~> route ~> check {
          assert(status == StatusCodes.BadRequest)
          assert(responseAs[StockManager.StockManagerResult] === ARTICLE_NOT_FOUND)
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Allow to sell an existing product if sufficiently available" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/sell", Quantity(5)) ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        checkArticles(1, List(product.copy(onStock = 95)), getProducts(route).get)
      }
    }

    "Deny to sell a non-existing product" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/2/sell", Quantity(5)) ~> route ~> check {
          assert(status == StatusCodes.BadRequest)
          assert(responseAs[StockManager.StockManagerResult] === ARTICLE_NOT_FOUND)
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Deny to buy an existing product if not sufficiently available" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/sell", Quantity(200)) ~> route ~> check {
          assert(status == StatusCodes.BadRequest)
          assert(responseAs[StockManager.StockManagerResult] === ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(product)))
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Allow to reserve a product if sufficiently available" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/reserve", Reservation(1, product.id, 5, 10.minutes.toMillis)) ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        val states = checkArticles(1, List(product), getProducts(route).get)
        assert(states.head.available == 95)
      }
    }

    "Deny to reserve a non-existing product" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/2/reserve", Reservation(1, 2, 5, 10.minutes.toMillis)) ~> route ~> check {
          assert(status == StatusCodes.BadRequest)
          assert(responseAs[StockManager.StockManagerResult] === ARTICLE_NOT_FOUND)
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Deny to reserve a product if not sufficiently available" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/reserve", Reservation(1, product.id, 200, 10.minutes.toMillis)) ~> route ~> check {
          assert(status == StatusCodes.BadRequest)
          assert(responseAs[StockManager.StockManagerResult] === ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(product)))
        }

        checkArticles(1, List(product), getProducts(route).get)
      }
    }

    "Allow to fulfill a reservation" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/reserve", Reservation(1, product.id, 90, 10.minutes.toMillis)) ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/reserve", Reservation(2, product.id, 10, 10.minutes.toMillis)) ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        val states = checkArticles(1, List(product), getProducts(route).get)
        assert(states.head.available == 0)

        Post(s"/reservations/${product.id}/fulfill") ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        val states2 = checkArticles(1, List(product.copy(onStock = 10)), getProducts(route).get)
        assert(states2.head.available == 0)
        assert(states2.length == 1)
      }
    }

    "Allow to cancel a reservation and make the quantity reserved available again" in {
      withRestService { route =>
        Post("/articles", product) ~> route ~> check {
          assert(responseAs[StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/reserve", Reservation(1, product.id, 10, 10.minutes.toMillis)) ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        Post(s"/articles/${product.id}/reserve", Reservation(2, product.id, 10, 10.minutes.toMillis)) ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        val states = checkArticles(1, List(product), getProducts(route).get)
        assert(states.head.available == 80)

        Delete(s"/reservations/${product.id}") ~> route ~> check {
          assert(responseAs[StockManager.StockManagerResult] === StockManagerResult(0))
        }

        val states2 = checkArticles(1, List(product), getProducts(route).get)
        assert(states2.head.available == 90)
      }
    }
  }
}
