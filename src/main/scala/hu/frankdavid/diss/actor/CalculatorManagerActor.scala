package hu.frankdavid.diss.actor

import akka.actor._
import hu.frankdavid.diss.expression._
import hu.frankdavid.diss.actor.CalculatorManagerActor._
import scala.collection.mutable
import hu.frankdavid.diss.util.LinkedHashSet
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.actor.WebSocketActor.SetCalculator
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import hu.frankdavid.diss.DataTable.UpdateResult
import hu.frankdavid.diss.actor.CalculatorManagerActor.Get
import hu.frankdavid.diss.actor.CalculatorManagerActor.ReceiveCalculationResult
import hu.frankdavid.diss.actor.CalculatorManagerActor.ScheduleCalculate
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.expression.Value
import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellBindingChanged
import hu.frankdavid.diss.actor.CalculatorManagerActor.Bind
import com.typesafe.config.Config
import hu.frankdavid.diss.event.CellValueChanged


class CalculatorManagerActor(socketActor: ActorRef) extends Actor with ActorLogging {

  import context._

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  private val valueCache = new DataTable


//  context.system.scheduler.schedule(0 milliseconds, 1000 milliseconds, self, IssueJobs)

  val worker = context.actorOf(Props(new CalculatorWorkerActor(self)))

  var router = {
    val routees = Vector.fill(8) {
      val r = context.actorOf(Props(new CalculatorWorkerActor(self)))
      context watch r
      r
    }
    akka.routing.RoundRobinRouter(routees)
  }

  socketActor ! SetCalculator

  self ! IssueJobs


  private var jobs = new LinkedHashSet[Expression]()
  private var jobsInTextTurn = new LinkedHashSet[Expression]()


  var c = 1

  def receive = {
    case ScheduleCalculate(expression) =>
      jobs += expression
    case Get(expression) =>
      sender ! valueCache.get(expression)
    case GetAllCells =>
      val list = valueCache.bindings.map {
        case (cell, hasValue) => (cell, (hasValue, valueCache.get(hasValue)))
      }.toList
      sender ! list
    case ReceiveCalculationResult(expression, result) =>
      val updateResult = valueCache.put(expression, result)
//      processUpdateResult(updateResult)
    case Bind(cell, expression) =>
      socketActor ! NotifyCellBindingChanged(cell, expression)
      val result = valueCache.bind(cell, expression)
      processUpdateResult(result)
    case IssueJobs =>
      sortJobsTopologically()
      var x = false
      jobs.foreach {
        job =>
          x = true
//          c += 1
//          log.info(c.toString)
          jobs.remove(job)
//          val params = job.dependencies.map(d => (d, valueCache.get(d)))
          val params = job.dependencies.map(valueCache.get(_).get)
//          val nulls = params.filter(_._2.isEmpty)
//          if (nulls.length > 0) {
//            log.info("miss")
//            nulls.map(n => valueCache.resolveExpression(n._1)).foreach {
//              case Some(e: Expression) =>
//                self ! ScheduleCalculate(e)
//              case _ =>
//            }
//          }
//          else {
//            val result = job.evaluate(params.map(_._2.get))
            val result = job.evaluate(params)
            val update = valueCache.put(job, result)
            processUpdateResult(update)
          }
          if(x)
            println("VÉGEEE")
//            worker ! Calculate(job, params.map(_._2.get))
//      }
      self ! IssueJobs
  }

  def processUpdateResult(updateResult: UpdateResult) {
    updateResult.notifiedExpressions.foreach {
      case c: Cell => valueCache.get(c).foreach {
        value =>
          socketActor ! CellValueChanged(c, value)
//          system.eventStream.publish(CellValueChanged(c, value))
      }
      case expr =>
        valueCache.resolveExpression(expr).map(self ! ScheduleCalculate(_))
    }
  }

  def sortJobsTopologically() {
    val visited = new mutable.HashSet[HasValue]()
    val visiting = new mutable.HashSet[HasValue]()
    val sorted = new LinkedHashSet[Expression]()
    def visit(expression: HasValue) {
      visiting += expression
      valueCache.listeners.getOrElse(expression, Set()).foreach {
        e =>
          if(e != expression && !visited.contains(e)) {
            if(visiting.contains(e))
              sys.error("Circular reference discovered")
            visit(e)

          }
      }
      visiting -= expression
      visited += expression
      expression match {
        case e: Expression => sorted += e
        case _ =>
      }
    }
    if (jobs.size > 0) {
      jobs.foreach(visit)
      jobs.clear()
      jobs ++= sorted.reverseIterator
    }
  }
//
//  def resolveExpression(expression: HasValue): Expression = {
//    expression match {
//      case c: Cell if bindings.contains(c) => resolveExpression(bindings(c))
//      case e: Expression => e
//      case _ => throw new IllegalArgumentException("Can't resolve " + expression.toString)
//    }
//  }

}


object CalculatorManagerActor {

  case class Get(expression: HasValue)

  case class ScheduleCalculate(expression: Expression)

  case class CancelCalculate(expression: Expression)

  case class ReceiveCalculationResult(expression: Expression, result: Value)

  case class RemoveAll(expression: Expression)

  case object WorkerIsIdle

  case object IssueJobs

  case class UpdateBindings(bindings: scala.collection.Map[Cell, HasValue])

  case class Bind(cell: Cell, expression: HasValue)

  case object GetAllCells

  class PrioMailbox (settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case ReceiveCalculationResult(_, _) => 0
      case IssueJobs => 1
      case otherwise => 2
    })


}