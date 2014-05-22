package hu.frankdavid.diss

import hu.frankdavid.diss.actor.{WebSocketHandler, CalculatorManager}
import hu.frankdavid.diss.expression.{Sum, Const, Cell}
import hu.frankdavid.diss.types.Matrix
import hu.frankdavid.diss.network.NetworkHandler
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import hu.frankdavid.diss.server.WebSocketServer
import akka.actor.ActorSystem

object Program extends App {
  val datatable = new DataTable()
  val network = new NetworkHandler()
  val socket = new WebSocketHandler()
  val socketServer = WebSocketServer.create(ActorSystem.create(), socket)
  socketServer.start()
  //  network.init()
  val manager = new CalculatorManager(socket, datatable, network)
  manager.bind(Cell(1, 2), Const(10L))
  manager.bind(Cell(1, 3), Const(4))
  manager.bind(Cell(1, 4), Const(
    new Matrix(
      Array[Any](1, 2, 3),
      Array[Any](4, 5, 14)
    )
  ))

  manager.bind(Cell(1, 6), Sum(Array(Cell(1, 2), Cell(1, 3))))

  manager.bind(Cell(2, 2), Sum(Array(Cell(1, 2), Cell(1, 3))))
  manager.bind(Cell(2, 3), Cell(2, 2))

  manager.bind(Cell(3, 0), Const(1))
  for(i <- 1 to 1000) {
    manager.bind(Cell(3, i), Sum(Array(Cell(3, i-1), Const(1))))
  }


//  val lotSums = for (i <- 0 to 4000) yield Cell(1, 4)
//  manager.calculate(Sum(lotSums.toArray)).onComplete {
//    case Success(result) => println("RESULT IS: " + result)
//    case Failure(throwable) => throwable.printStackTrace()
//  }

  println("Calc started")

  //  Thread.sleep(3000)
  //  println(manager.get(Cell(2, 2)))
  //  println(manager.get(Cell(1, 4)))

  readLine()

  //  val sum = Sum(Seq.fill(10000000)(Cell(1, 4)))
  //  private val count = CountTo(Array(Cell(1, 2)))
  //  network.sendCalculation(count)
  //  println("Local result: " +  count.evaluate(datatable))
}