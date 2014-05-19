package hu.frankdavid.diss.multithreading_performance_test

import akka.actor.{Props, ActorSystem, Actor}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import scala.concurrent._
import ExecutionContext.Implicits.global

object ActorTest extends App {
  val system = ActorSystem.create()
  val actor = system.actorOf(Props[TestActor])
  val routees = Vector.fill(4) {
    val r = system.actorOf(Props[TestActor])
    ActorRefRoutee(r)
  }
  val router = Router(RoundRobinRoutingLogic(), routees)
  val start = System.currentTimeMillis()
  val numbers = (1 to 100000).toList
  for (i <- 0 to 100000) {
        router.route(Count(i, numbers), null)
    //    actor ! Count(i, numbers)
//    future {
//      numbers.sum
//      if (i % 10000 == 0)
//        println(System.currentTimeMillis() - start)
////    }
  }

  class TestActor extends Actor {
    def receive = {
      case Count(i, numbers) =>
        numbers.sum
        if (i % 10000 == 0)
          println(System.currentTimeMillis() - start)
    }
  }

  case class Count(i: Integer, numbers: Seq[Int])

}