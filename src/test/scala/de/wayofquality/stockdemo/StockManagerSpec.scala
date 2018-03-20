package de.wayofquality.stockdemo

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActor, TestKit}
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

  def withStockManager(f: ActorRef => Unit) = {

    val testActor = system.actorOf(StockManager.props())
    f(testActor)
    system.stop(testActor)
  }

  "The StockManager should" - {

    "Allow to create a product along with an available Quantity" in {

      withStockManager{testActor =>
        val product = Article("Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0, None))

        testActor ! ListArticles

        fishForMessage(1.second){
          case Stock(l) =>
            log.info(s"Articles: $l")
            l.length == 1 && l.head.name === "Super Computer" && l.head.id == product.id
          case _ => false
        }
      }
    }

    "Deny to create a product with an id that already exists" in {

      withStockManager { testActor =>
        val product = Article("Super Computer", 10)

        testActor ! CreateArticle(product)
        expectMsg(StockManagerResult(0, None))

        testActor ! CreateArticle(product)
        expectMsg(ARTICLE_ALREADY_EXISTS)

        testActor ! ListArticles

        fishForMessage(1.second){
          case Stock(l) =>
            log.info(s"Articles: $l")
            l.length == 1 && l.head.name === "Super Computer" && l.head.id == product.id
          case _ => false
        }
      }
    }

    "Allow to increase the available quantity of an existing product" in {
      pending
    }

    "Deny to increase the available quantity of a non-existing product" in {
      pending
    }

    "Allow to buy an existing product if sufficiently available" in {
      pending
    }

    "Deny to buy an existing product if not sufficiently available" in {
      pending
    }

    "Allow to reserve a product if sufficiently available" in {
      pending
    }

    "Deny to reserve a product if not sufficiently available" in {
      pending
    }

    "Allow to cancel a reservation and make the quantity reserved available again" in {
      pending
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
