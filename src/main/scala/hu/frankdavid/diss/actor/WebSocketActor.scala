package hu.frankdavid.diss.actor

import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import io.netty.channel.Channel
import hu.frankdavid.diss.expression.{Const, HasValue, Value, Cell}
import hu.frankdavid.diss.actor.WebSocketActor._
import hu.frankdavid.diss.Program
import scala.util.parsing.json.{JSONArray, JSONObject, JSON}
import hu.frankdavid.diss.actor.TableHandlerActor.{GetAllCells, Bind}
import java.lang.NumberFormatException
import akka.pattern._
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import hu.frankdavid.diss.actor.WebSocketActor.ReceiveCellBindingChanged
import hu.frankdavid.diss.expression.Value
import scala.util.parsing.json.JSONArray
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellValueChanged
import scala.util.parsing.json.JSONObject
import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellBindingChanged
import scala.Some
import hu.frankdavid.diss.actor.TableHandlerActor.Bind
import scala.concurrent.duration._


class WebSocketActor extends Actor with ActorLogging {

  import context._

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  val messages = ListBuffer[String]()
  var tableActor: Option[ActorRef] = _

  system.scheduler.schedule(0 milliseconds, 100 milliseconds, self, Flush)

  def receive = {
    case SetTable =>
      tableActor = Some(sender)
    case Flush =>
      if(messages.length > 0) {
        Program.webServer.webSocketConnections.writeText(messages.mkString("[", ",", "]"))
        messages.clear()
      }

    case NotifyCellValueChanged(cell, value) =>
      messages += "{\"cell\": [" + cell.row + ", " + cell.col + "], \"value\": \"" + value.value + "\"}"
    case NotifyCellBindingChanged(cell, expression) =>
      messages += "{\"cell\": [" + cell.row + ", " + cell.col + "], \"binding\": \"" + expressionToString(expression) + "\"}"
    case ReceiveCellBindingChanged(bindingString) =>
      JSON.parseRaw(bindingString) match {
        case Some(o) =>
          val json = o.asInstanceOf[JSONObject].obj
          val cellarr = json("cell").asInstanceOf[JSONArray].list
          val cell = Cell(cellarr(0).asInstanceOf[Double].toInt, cellarr(1).asInstanceOf[Double].toInt)
          val expressionString = json("binding").asInstanceOf[String]
          val parse = parseExpression(expressionString)
          parse match {
            case Some(expr) =>  tableActor.map(_ ! Bind(cell, expr))
            case _ =>
          }
      }
    case PushAllCells =>
      val maybeAllCells = tableActor.map(_ ? GetAllCells)
      maybeAllCells match {
        case Some(allCells) => allCells.mapTo[List[(Cell, (HasValue, Option[Value]))]].onSuccess {
          case list => list.foreach {
            case (cell, (expression, maybeValue)) =>
              self ! NotifyCellBindingChanged(cell, expression)
              maybeValue match {
                case Some(value) => self ! NotifyCellValueChanged(cell, value)
                case _ =>
              }
          }
        }
      }

  }

  private def expressionToString(expression: HasValue): String = expression match {
    case c: Const => c.value.value.toString
    case _ => "=" + expression
  }

  private def parseExpression(expressionString: String): Option[HasValue] = {
    try {
      Some(Const(expressionString.toInt))
    } catch {
      case e: NumberFormatException => try {
        Some(Const(expressionString.toDouble))
      } catch {
        case e: NumberFormatException => None
      }
    }
  }
}

object WebSocketActor {

  case class WebSocketRegistered(channel: Channel)

  case class NotifyCellValueChanged(cell: Cell, value: Value)
  
  case class NotifyCellBindingChanged(cell: Cell, expression: HasValue)

  case class ReceiveCellBindingChanged(bindingString: String)

  case object PushAllCells

  case object Flush

  case object SetTable
}