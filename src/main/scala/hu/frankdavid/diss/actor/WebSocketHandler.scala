package hu.frankdavid.diss.actor

import io.netty.channel.Channel
import hu.frankdavid.diss.expression._
import java.lang.NumberFormatException
import scala.collection.mutable.ListBuffer
import scala.collection
import scala.Some
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.Program
import scala.util.parsing.json.{JSONArray, JSONObject, JSON}
import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellBindingChanged
import org.mashupbots.socko.webserver.WebServer
import scala.concurrent.{ExecutionContext, future}
import ExecutionContext.Implicits.global


class WebSocketHandler() {

  var server: WebServer = _
  val messages = ListBuffer[String]()
  var calculatorManager: CalculatorManager = _

  var bindingChanges = new collection.mutable.HashMap[Cell, HasValue]
  var valueChanges = new collection.mutable.HashMap[Cell, Any]

  def cellBindingChanged(cell: Cell, expression: HasValue) {
    synchronized {
      bindingChanges(cell) = expression
    }
  }

  def cellBindingChanged(bindingString: String) {
    JSON.parseRaw(bindingString) match {
      case Some(o) =>
        val json = o.asInstanceOf[JSONObject].obj
        val cellarr = json("cell").asInstanceOf[JSONArray].list
        val cell = Cell(cellarr(0).asInstanceOf[Double].toInt, cellarr(1).asInstanceOf[Double].toInt)
        val expressionString = json("binding").asInstanceOf[String]
        val parse = parseExpression(expressionString)
        parse match {
          case Some(expr) => calculatorManager.bind(cell, expr)
          case _ =>
        }
  }
}

  def cellValueChanged(cell: Cell, value: Any) {
    synchronized {
      valueChanges(cell) = value
    }
  }

  def pushAllCells() {
    val list = calculatorManager.getAllCells()
    list.foreach {
      case (cell, (expression, maybeValue)) =>
        cellBindingChanged(cell, expression)
        maybeValue match {
          case Some(value) => cellValueChanged(cell, value)
          case _ =>
        }
    }
  }

  def receiveCellBindingChanged(bindingString: String) {
    JSON.parseRaw(bindingString) match {
      case Some(o) =>
        val json = o.asInstanceOf[JSONObject].obj
        val cellarr = json("cell").asInstanceOf[JSONArray].list
        val cell = Cell(cellarr(0).asInstanceOf[Double].toInt, cellarr(1).asInstanceOf[Double].toInt)
        val expressionString = json("binding").asInstanceOf[String]
        val parse = parseExpression(expressionString)
        parse match {
          case Some(expr) => calculatorManager.bind(cell, expr, true)
          case _ =>
        }
    }
  }

  def flush() {
    if (valueChanges.size > 0 || bindingChanges.size > 0) {
      val valueChanges = this.valueChanges
      val bindingChanges = this.bindingChanges
      this.valueChanges = new collection.mutable.HashMap[Cell, Any]
      this.bindingChanges = new collection.mutable.HashMap[Cell, HasValue]
      val valueStringParts = valueChanges.map {
        case (cell, value) => "{\"cell\": [" + cell.row + ", " + cell.col + "], \"value\": \"" + value.toString + "\"}"
      }
      val bindingStringParts = bindingChanges.map {
        case (cell, expression) => "{\"cell\": [" + cell.row + ", " + cell.col + "], \"binding\": \"" + expressionToString(expression) + "\"}"
      }
      server.webSocketConnections.writeText((bindingStringParts ++ valueStringParts).mkString("[", ",", "]"))
    }
  }

  future {
    while(true) {
      flush()
      Thread.sleep(100)
    }
  }

  def escape(string: String) {
    string.replace("\n", "\\n").replace("\t", "\\t")
  }

  //  def receive = {
  //    case Flush =>
//        if(valueChanges.size > 0 || bindingChanges.size > 0) {
//          val valueStringParts = valueChanges.map {
//            case (cell, value) => "{\"cell\": [" + cell.row + ", " + cell.col + "], \"value\": \"" + value.value + "\"}"
//          }
//          val bindingStringParts = bindingChanges.map {
//            case (cell, expression) => "{\"cell\": [" + cell.row + ", " + cell.col + "], \"binding\": \"" + expressionToString(expression) + "\"}"
//          }
//          Program.webServer.webSocketConnections.writeText((bindingStringParts ++ valueStringParts).mkString("[", ",", "]"))
  //        valueChanges.clear()
  //        bindingChanges.clear()
  //      }
  //    case CellValueChanged(cell, value) =>
  //      valueChanges(cell) = value
  ////      Program.webServer.webSocketConnections.writeText((bindingStringParts ++ valueStringParts).mkString("[", ",", "]"))
  ////      Program.webServer.webSocketConnections.writeText("[{\"cell\": [" + cell.row + ", " + cell.col + "], \"value\": \"" + value.value + "\"}]")
  //    case NotifyCellBindingChanged(cell, expression) =>
  //      bindingChanges(cell) = expression
  ////      messages += "{\"cell\": [" + cell.row + ", " + cell.col + "], \"binding\": \"" + expressionToString(expression) + "\"}"
  ////      Program.webServer.webSocketConnections.writeText("[{\"cell\": [" + cell.row + ", " + cell.col + "], \"binding\": \"" + expressionToString(expression) + "\"}]")
//      case ReceiveCellBindingChanged(bindingString) =>
//        JSON.parseRaw(bindingString) match {
//          case Some(o) =>
//            val json = o.asInstanceOf[JSONObject].obj
//            val cellarr = json("cell").asInstanceOf[JSONArray].list
//            val cell = Cell(cellarr(0).asInstanceOf[Double].toInt, cellarr(1).asInstanceOf[Double].toInt)
//            val expressionString = json("binding").asInstanceOf[String]
//            val parse = parseExpression(expressionString)
//            parse match {
//              case Some(expr) => calculatorActor.map(_ ! Bind(cell, expr))
//              case _ =>
//            }
//        }
  //    case PushAllCells =>
  //      val maybeAllCells = calculatorActor.map(_ ? GetAllCells)
  //      maybeAllCells match {
  //        case Some(allCells) => allCells.mapTo[List[(Cell, (HasValue, Option[Value]))]].onSuccess {
  //          case list => list.foreach {
  //            case (cell, (expression, maybeValue)) =>
  //              self ! NotifyCellBindingChanged(cell, expression)
  //              maybeValue match {
  //                case Some(value) => self ! CellValueChanged(cell, value)
  //                case _ =>
  //              }
  //          }
  //        }
  //      }
  //
  //  }

  private def expressionToString(expression: HasValue): String = expression match {
    case c: Const => c.value.toString
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

  case class NotifyCellBindingChanged(cell: Cell, expression: HasValue)

  case class ReceiveCellBindingChanged(bindingString: String)

  case object PushAllCells

  case object Flush

  case object SetCalculator

}