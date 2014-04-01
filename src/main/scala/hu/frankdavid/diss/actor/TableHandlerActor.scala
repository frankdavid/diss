//package hu.frankdavid.diss.actor
//
//import akka.actor.{ActorLogging, ActorRef, Props, Actor}
//import hu.frankdavid.diss.DataTable
//import hu.frankdavid.diss.actor.TableHandlerActor._
//import hu.frankdavid.diss.expression.{Value, HasValue, Cell, Expression}
//import hu.frankdavid.diss.DataTable.UpdateResult
//import hu.frankdavid.diss.actor.WebSocketActor.{SetTable, NotifyCellBindingChanged, NotifyCellValueChanged}
//import hu.frankdavid.diss.actor.TableHandlerActor.Put
//import hu.frankdavid.diss.expression.Value
//import hu.frankdavid.diss.expression.Cell
//import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellValueChanged
//import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellBindingChanged
//import hu.frankdavid.diss.DataTable.UpdateResult
//import hu.frankdavid.diss.actor.TableHandlerActor.Get
//import scala.Some
//import hu.frankdavid.diss.actor.TableHandlerActor.Bind
//import scala.collection.{immutable, mutable}
//import scala.collection.immutable.HashMap
//import hu.frankdavid.diss.actor.CalculatorManagerActor.UpdateBindings
//
//class TableHandlerActor(socketActor: ActorRef) extends Actor with ActorLogging {
//  private val valueCache = new DataTable
//  private val calculatorActor = context.actorOf(Props(new CalculatorManagerActor(self)))
//  var put = 0L
//
//  socketActor ! SetTable
//
//  def receive = {
//    case Get(expression) =>
//      sender ! valueCache.get(expression)
//    case GetAllCells =>
//      val list = valueCache.bindings.map {
//        case (cell, hasValue) => (cell, (hasValue, valueCache.get(hasValue)))
//      }.toList
//      sender ! list
//    case Put(expression, value) =>
//      val result = valueCache.put(expression, value)
//      processUpdateResult(result)
//    case Bind(cell, expression) =>
//      socketActor ! NotifyCellBindingChanged(cell, expression)
//      val result = valueCache.bind(cell, expression)
//      calculatorActor ! UpdateBindings(valueCache.bindings)
//      processUpdateResult(result)
//  }
//
//  def processUpdateResult(updateResult: UpdateResult) {
//    updateResult.notifiedExpressions.foreach {
//      case expression: Expression =>
////        if(expression.dependencies.forall(dep => valueCache.get(dep).isDefined))
//          calculatorActor ! CalculatorManagerActor.ScheduleCalculate(expression)
//      case cell : Cell =>
//        val maybeValue = valueCache.get(cell)
////        val message = s"Cell updated: $cell = " + maybeValue
//        maybeValue match {
//          case Some(value) => socketActor ! NotifyCellValueChanged(cell, value)
//          case _ =>
//        }
//      case _ =>
//    }
//  }
//
//}
//
//object TableHandlerActor {
//  case class Get(expression: HasValue)
//  case class Put(expression: Expression, value: Value)
//  case class Bind(cell: Cell, expression: HasValue)
//  case object GetAllCells
//}
