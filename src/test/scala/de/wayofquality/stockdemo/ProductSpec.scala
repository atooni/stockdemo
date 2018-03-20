package de.wayofquality.stockdemo

import org.scalatest.{FreeSpec, Matchers}

class ProductSpec extends FreeSpec with Matchers {

  "A product" - {

    "should always have a quantity >= 0" in {

      val p = Product("Foo", 100)

      assert(p.name === "Foo")
      assert(p.onStock === 100)

      an [Exception] should be thrownBy ( Product("Bar", -3))
    }
  }

}
