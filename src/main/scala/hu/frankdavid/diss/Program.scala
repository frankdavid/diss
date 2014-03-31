package hu.frankdavid.diss

import akka.actor.{Props, ActorSystem}
import hu.frankdavid.diss.actor.{WebSocketActor, TableHandlerActor}
import hu.frankdavid.diss.expression.Const
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.expression.Sum
import hu.frankdavid.diss.actor.TableHandlerActor.Bind
import hu.frankdavid.diss.server.DissServer

//import hu.frankdavid.diss.actor.WebSocketActor.WebSocketRegistered

object Program extends App {
  private val system = ActorSystem.create("diss")
  private val socket = system.actorOf(Props[WebSocketActor])
  private val table = system.actorOf(Props(new TableHandlerActor(socket)), "table")

  val webServer = DissServer.create(system, socket)

  webServer.start()
  println("start")

  for(i <- 0 to 10) {
    table ! Bind(Cell(i, 0), Const(1))
    table ! Bind(Cell(0, i), Const(1))
  }

  for(row <- 1 to 10; col <- 1 to 10) {
    table ! Bind(Cell(row, col), Sum(Seq(Cell(row, col - 1), Cell(row - 1, col))))
  }


  table ! Bind(Cell(0, 1), Const(5))


}