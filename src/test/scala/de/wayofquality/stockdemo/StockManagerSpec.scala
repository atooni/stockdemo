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
  ) : Unit = fishForMessage(1.second){
    case Stock(l) =>
      log.info(s"Articles: $l")
      (l.length == articleCount) && contained.forall { a => l.map(_.article).contains(a)}
    case _ => false
  }

  "The StockManager should" - {

    "Allow to create a product along with an available Quantity" in {

      withStockManager{testActor =>
        val a1 = Article("Super Computer", 10)
        val a2 = Article("Another cool product", 200)

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
        val product = Article("Super Computer", 10)

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
        val product = Article("Super Computer", 10)

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
         val product = Article("Super Computer", 10)

        testActor ! Refill(product.id, 10)
        expectMsg(ARTICLE_DOES_NOT_EXIST)
      }
    }

    "Allow to buy an existing product if sufficiently available" in {
      withStockManager { testActor =>
        val product = Article("Super Computer", 10)

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
        val product = Article("Super Computer", 10)

        testActor ! Sell(product.id, 10)
        expectMsg(ARTICLE_DOES_NOT_EXIST)
      }
    }

    "Deny to buy an existing product if not sufficiently available" in {
      withStockManager { testActor =>
        val product = Article("Super Computer", 10)

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
        val product = Article("Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0))

        testActor ! Reservation(product.id, 5, 10.minutes)
        expectMsg(StockManagerResult(0))

        testActor ! ListArticles
        checkArticles(1, List(product.copy(onStock = 5)))
      }
    }

    "Deny to reserve a non-existing product" in {
      pending
    }

    "Deny to reserve a product if not sufficiently available" in {
      withStockManager{ testActor =>
        val product = Article("Super Computer", 10)

        testActor ! Reservation(product.id, 10, 10.minutes)
        expectMsg(ARTICLE_DOES_NOT_EXIST)
      }
    }

    "Allow to cancel a reservation and make the quantity reserved available again" in {
      pending
    }

    "Allow to cancel a given reservation" in {
      pending
    }

    "Take into account the current reservations when checking the current stock" in {
      pending
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
