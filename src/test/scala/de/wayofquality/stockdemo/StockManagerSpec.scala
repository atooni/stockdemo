package de.wayofquality.stockdemo

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike}

import scala.concurrent.duration._

//The Task
//************
//The task is to develop a stock management as a Rest-Microservice in Scala. A stock management maintains the current
// stock amount for a product. The stock can be increased (refill) or decreased (client buys a product). The stock
// should be maintained on a per-product basis. If you are quick you can also implement a "reserve product" mechanism,
// that blocks a product in the stock until the client actually buys it (similar to the seat reservation system for
// flight tickets). However this is optional.
//
// The project should be checked into a public code repository of your choice like Github, Bitbucket or Gitlab.
// It must contain tests and run out of the box, i.e. it should not require manual installation of other components.
// Use a build system of your choice (e.g. Maven, SBT, Gradle).
//************

class StockManagerSpec extends TestKit(ActorSystem("stock"))
  with FreeSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  import StockManager._

  private[this] val log = org.log4s.getLogger

  // convenience method to run a test against a fresh StockManager and stopping it afterwards
  def withStockManager(f: ActorRef => Unit) : Unit = {

    val testActor = system.actorOf(StockManager.props())
    f(testActor)
    system.stop(testActor)
  }

  // convenenience method to check the result of the current stock state
  // not taking reservations into account
  def checkArticles(
    articleCount : Long,
    contained : List[Article]
  ) : List[ArticleState] = {
    val stock = expectMsgType[Stock]
    val l = stock.products.map(_.article)

    log.info(s"Product State [$stock], Checking for products [$l]")
    assert(
      l.length == articleCount &&
      contained.forall { a => l.contains(a) }
    )

    stock.products
  }

  "The StockManager should" - {

    "Allow to create a product along with an available Quantity" in {

      withStockManager{testActor =>
        val a1 = Article(1, "Super Computer", 10)
        val a2 = Article(2, "Another cool product", 200)

        testActor ! CreateArticle(a1)
        expectMsg(StockManagerResult(0, None))

        testActor ! CreateArticle(a2)
        expectMsg(StockManagerResult(0, None))

        testActor ! ListArticles
        checkArticles(2, List(a1, a2))
      }
    }

    "Deny to create a product with an id that already exists" in {

      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0, None))

        testActor ! CreateArticle(product)
        expectMsg(ARTICLE_ALREADY_EXISTS.copy(article = Some(product)))

        testActor ! ListArticles
        checkArticles(1, List(product))
      }
    }

    "Allow to increase the available quantity of an existing product" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! Refill(product.id, 10)
        expectMsg(StockManagerResult(0))

        testActor ! ListArticles
        checkArticles(1, List(product.copy(onStock = 20)))
      }
    }

    "Deny to increase the available quantity of a non-existing product" in {
      withStockManager{ testActor =>
         val product = Article(1, "Super Computer", 10)

        testActor ! Refill(product.id, 10)
        expectMsg(ARTICLE_NOT_FOUND)
      }
    }

    "Allow to buy an existing product if sufficiently available" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! Sell(product.id, 5)
        expectMsg(StockManagerResult(0))

        testActor ! ListArticles
        checkArticles(1, List(product.copy(onStock = 5)))


      }
    }

    "Deny to buy a non-existing product" in {
      withStockManager{ testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! Sell(product.id, 10)
        expectMsg(ARTICLE_NOT_FOUND)
      }
    }

    "Deny to buy an existing product if not sufficiently available" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! Sell(product.id, 20)
        expectMsg(ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(product)))

        testActor ! ListArticles
        checkArticles(1, List(product))
      }

    }

    "Allow to reserve a product if sufficiently available" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! Reservation(1, product.id, 5, 10.minutes.toMillis)
        expectMsg(StockManagerResult(0))

        testActor ! ListArticles
        val stock = checkArticles(1, List(product.copy(onStock = 10)))
        assert(stock.head.available == 5)

      }
    }

    "Deny to reserve a non-existing product" in {
      withStockManager{ testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! Reservation(1, product.id, 10, 10.minutes.toMillis)
        expectMsg(ARTICLE_NOT_FOUND)
      }
    }

    "Deny to reserve a product if not sufficiently available" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! Reservation(1, product.id, 5, 10.minutes.toMillis)
        expectMsg(StockManagerResult(0))

        testActor ! Reservation(2, product.id, 10, 10.minutes.toMillis)
        expectMsg(ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(product)))

        testActor ! ListArticles
        val stock = checkArticles(1, List(product.copy(onStock = 10)))
        assert(stock.head.available == 5)
      }
    }

    "Allow to fulfill a given reservation" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)
        val reservation = Reservation(1, product.id, 5, 10.minutes.toMillis)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! reservation
        expectMsg(StockManagerResult(0))

        testActor ! FulfillReservation(reservation.id)
        expectMsg(StockManagerResult(0))

        testActor ! ListArticles
        val stock = checkArticles(1, List(product.copy(onStock = 5)))
        assert(stock.head.available == 5)
        assert(stock.head.reservations.isEmpty)
      }
    }

    "Allow to cancel a reservation and make the quantity reserved available again" in {
      withStockManager { testActor =>
        val product = Article(1, "Super Computer", 10)
        val reservation = Reservation(1, product.id, 5, 10.minutes.toMillis)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! reservation
        expectMsg(StockManagerResult(0))

        testActor ! CancelReservation(reservation.id)
        expectMsg(StockManagerResult(0))

        testActor ! ListArticles
        val stock = checkArticles(1, List(product.copy(onStock = 10)))
        assert(stock.head.reservations.isEmpty)
        assert(stock.head.available == 10)
      }
    }

    "Take into account the current reservations when checking the current stock" in {
      withStockManager { testActor =>
        // Initially we have 10
        val product = Article(1, "Super Computer", 10)
        val res1 = Reservation(1, product.id, 3, 10.minutes.toMillis)
        val res2 = Reservation(2, product.id, 4, 10.minutes.toMillis)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        // Reserve 3
        testActor ! res1
        expectMsg(StockManagerResult(0))

        // Reserve another 4
        testActor ! res2
        expectMsg(StockManagerResult(0))

        // try to sell 5 - not working (only 3 free)
        testActor ! Sell(product.id, 5)
        expectMsg(ARTICLE_UNSUFFICIENT_STOCK.copy(article = Some(product)))

        // cancel one reservation
        testActor ! CancelReservation(res1.id)
        expectMsg(StockManagerResult(0))

        // now sell 5
        testActor ! Sell(product.id, 5)
        expectMsg(StockManagerResult(0))

        // Now we should have 1 freely available and 5 on stock
        testActor ! ListArticles
        val stock = checkArticles(1, List(product.copy(onStock = 5)))
        assert(stock.head.available == 1)
      }
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
