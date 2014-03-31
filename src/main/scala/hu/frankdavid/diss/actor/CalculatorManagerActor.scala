package hu.frankdavid.diss.actor

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern._
import scala.concurrent.Future
import scala.concurrent.duration._
import hu.frankdavid.diss.expression._
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import hu.frankdavid.diss.actor.TableHandlerActor.Put
import hu.frankdavid.diss.actor.TableHandlerActor.Get
import hu.frankdavid.diss.actor.CalculatorManagerActor.{RemoveAll, CancelCalculate, Calculate}
import scala.collection

class CalculatorManagerActor(valueCacheActor: ActorRef) extends Actor with ActorLogging {

  import context._

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  var c = 0L

  val jobs = collection.mutable.HashSet[Expression]()

//  system.scheduler.schedule(0 milliseconds, 1000 milliseconds, self, RemoveAll)

  def receive = {
    case Calculate(expression) =>
      if(!jobs.contains(expression)) {
        jobs.add(expression)
        calculate(expression)
        c += 1
        log.info(c.toString)
      }
    case CancelCalculate(expression) =>
      jobs -= expression
//    case RemoveAll =>
//      jobs.clear()

  }

  def calculate(expression: Expression) {
    if(expression == Sum(List(Cell(3,2), Cell(2,3))))
      log.info(expression.toString)
    val params = expression.dependencies.map {
      param => (valueCacheActor ? Get(param)).mapTo[Option[Value]].map((param, _))
    }
    Future.sequence[(HasValue, Option[Value]), Seq](params).onSuccess {
      case results =>
        val nulls = results.filter(_._2 == None)
        if (nulls.length > 0)
          nulls.foreach {
            case (exp: Expression, _) => self ! Calculate(exp)
            case _ =>
          }
        else
          expression match {
            case e: Expression =>
              val result = e.evaluate(results.map(_._2.get))
              valueCacheActor ! Put(e, result)
            case _ =>
          }
        self ! CancelCalculate(expression)
    }
  }


}


object CalculatorManagerActor {
  case class Calculate(expression: Expression)
  case class CancelCalculate(expression: Expression)
  case class RemoveAll(expression: Expression)
}