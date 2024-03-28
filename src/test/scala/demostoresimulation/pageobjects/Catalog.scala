package demostoresimulation.pageobjects

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Catalog {

  private val categoryFeeder = csv("data/categoryDetails.csv").random
  private val jsonProductFeeder = jsonFile("data/productDetails.json").random

  object Category {
    def view = {
      feed(categoryFeeder)
        .exec(http("Load Category Page - #{categoryName}")
          .get("/category/#{categorySlug}")
          .check(status.is(200))
          .check(css("#CategoryName").is("#{categoryName}")))
    }
  }

  object Product {
    def view = {
      feed(jsonProductFeeder)
        .exec(http("Load Product Page - #{name}")
          .get("/product/#{slug}")
          .check(status.is(200))
          .check(css("#ProductDescription").is("#{description}")))
    }

    def add = {
      exec(view)
        .exec(http("Add Product to Cart")
          .get("/cart/add/#{id}")
          .check(status.is(200))
          .check(substring("items in your cart")))
        .exec(session => {
          val currentCartTotal = session("cartTotal").as[Double]
          val itemPrice = session("price").as[Double]
          session.set("cartTotal", (currentCartTotal + itemPrice))
        })
//          .exec { session => println(session); session }
    }
  }
}
