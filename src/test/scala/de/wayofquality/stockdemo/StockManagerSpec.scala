package de.wayofquality.stockdemo

import org.scalatest.FreeSpec

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

class StockManagerSpec extends FreeSpec {

  "The StockManager should" - {

    "Allow to define a product along with an available Quantity" in {
      pending
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

}
