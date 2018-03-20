package de.wayofquality.stockdemo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.io.StdIn

object StockManagerMain {

  private[this] val log = org.log4s.getLogger

  def main(args: Array[String]) : Unit = {

    implicit val system = ActorSystem("StockManager")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val port = system.settings.config.getInt("de.wayofquality.stockdemo.port")

    val bindingFuture = Http().bindAndHandle(StockManagerService.route, "localhost", port)

    println(s"Server online at http://localhost:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
