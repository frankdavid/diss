package hu.frankdavid.diss.actor

import akka.actor._
import scala.concurrent.duration._
import hu.frankdavid.diss.expression._
import hu.frankdavid.diss.actor.CalculatorManagerActor._
import hu.frankdavid.diss.actor.CalculatorManagerActor.ScheduleCalculate
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import hu.frankdavid.diss.util.LinkedHashSet
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.DataTable.UpdateResult
import hu.frankdavid.diss.actor.WebSocketActor.{CellValueChanged, SetCalculator, NotifyCellBindingChanged}
import hu.frankdavid.diss.actor.CalculatorWorkerActor.Calculate
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import hu.frankdavid.diss.DataTable.UpdateResult
import hu.frankdavid.diss.actor.CalculatorManagerActor.Get
import scala.Some
import hu.frankdavid.diss.actor.CalculatorManagerActor.ReceiveCalculationResult
import hu.frankdavid.diss.actor.CalculatorManagerActor.ScheduleCalculate
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.expression.Value
import hu.frankdavid.diss.actor.WebSocketActor.CellValueChanged
import hu.frankdavid.diss.actor.WebSocketActor.NotifyCellBindingChanged
import hu.frankdavid.diss.actor.CalculatorWorkerActor.Calculate
import hu.frankdavid.diss.actor.CalculatorManagerActor.Bind
import com.typesafe.config.Config


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
      jobs.foreach {
        job =>
//          c += 1
//          log.info(c.toString)
          jobs.remove(job)
          val params = job.dependencies.map(d => (d, valueCache.get(d)))
          val nulls = params.filter(_._2.isEmpty)
          if (nulls.length > 0) {
            log.info("miss")
//            nulls.map(n => valueCache.resolveExpression(n._1)).foreach {
//              case Some(e: Expression) =>
//                self ! ScheduleCalculate(e)
//              case _ =>
//            }
          }
          else {
            val result = job.evaluate(params.map(_._2.get))
            val update = valueCache.put(job, result)
            processUpdateResult(update)
          }
//            worker ! Calculate(job, params.map(_._2.get))
      }
      self ! IssueJobs
  }

  def processUpdateResult(updateResult: UpdateResult) {
    updateResult.notifiedExpressions.foreach {
      case c: Cell => valueCache.get(c).foreach {
            system.eventStream.publish(CellValueChanged)
        value => socketActor ! CellValueChanged(c, value)
      }
//        valueCache.resolveExpression(c).map(self ! ScheduleCalculate(_))
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