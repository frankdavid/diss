package hu.frankdavid.diss.actor

import hu.frankdavid.diss.expression._
import akka.actor.{ActorRef, Actor}
import scala.concurrent.Future
import hu.frankdavid.diss.expression.Value
import hu.frankdavid.diss.actor.CalculatorManagerActor.{Get, ReceiveCalculationResult, ScheduleCalculate, CancelCalculate}
import hu.frankdavid.diss.actor.CalculatorWorkerActor.Calculate
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import akka.pattern._


class CalculatorWorkerActor(calculatorManagerActor: ActorRef) extends Actor {

  import context._

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  def receive = {
    case Calculate(expression, parameters) => calculate(expression, parameters)
  }

  def calculate(expression: Expression, parameters: Seq[Value]) {
        val result = expression.evaluate(parameters)
        calculatorManagerActor ! ReceiveCalculationResult(expression, result)
  }

//  def calculate(expression: Expression, parameters: Seq[Value]) {
//    val params = expression.dependencies.map {
//      param => (calculatorManagerActor ? Get(param)).mapTo[Option[Value]].map((param, _))
//    }
//    Future.sequence[(HasValue, Option[Value]), Seq](params).onSuccess {
//      case results =>
//        val nulls = results.filter(_._2 == None)
//        if (nulls.length > 0) {
//          nulls.foreach {
//            case (exp: Expression, _) => sender ! ScheduleCalculate(exp)
//            case _ =>
//          }
//          sender ! ScheduleCalculate(expression)
//        }
//        else
//          expression match {
//            case e: Expression =>
//              val result = e.evaluate(results.map(_._2.get))
//              calculatorManagerActor ! ReceiveCalculationResult(e, result)
//            case _ =>
//          }
//    }
//  }

}



object CalculatorWorkerActor {

  case class Calculate(expression: Expression, parameters: Seq[Value])

}