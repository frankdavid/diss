package hu.frankdavid.diss

import akka.actor.{Props, ActorSystem}
import hu.frankdavid.diss.actor.{CalculatorManagerActor, WebSocketActor}
import hu.frankdavid.diss.expression.Const
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.expression.Sum
import hu.frankdavid.diss.server.DissServer
import hu.frankdavid.diss.actor.CalculatorManagerActor.Bind

object Program extends App {
  private val system = ActorSystem.create("diss")
  private val socket = system.actorOf(Props[WebSocketActor])
  private val calculator = system.actorOf(Props(new CalculatorManagerActor(socket)))

  val webServer = DissServer.create(system, socket)

  webServer.start()
  val Size = 50

  for(i <- 0 to Size) {
    calculator ! Bind(Cell(i, 0), Const(1))
    calculator ! Bind(Cell(0, i), Const(1))
  }

  for(row <- 1 to Size; col <- 1 to Size) {
    calculator ! Bind(Cell(row, col), Sum(Seq(Cell(row, col - 1), Cell(row - 1, col))))
  }


  calculator ! Bind(Cell(0, 1), Const(5))


}