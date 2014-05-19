package hu.frankdavid.diss.multithreading_performance_test

import scala.concurrent.forkjoin.{RecursiveTask, ForkJoinPool}

object ForkJoinTest extends PerformanceTest {

  def runTest() {
    val pool = new ForkJoinPool()

    val result = pool.invoke(new SumTask(0, 10000000000L))
    println(result)
  }
}

class SumTask(from: Long, to: Long) extends RecursiveTask[Long] {
  def compute(): Long = {
    if (to - from > 1000000L) {
      val t1 = new SumTask(from, (to + from) / 2)
      val t2 = new SumTask((to + from) / 2, to)

      t2.fork()

      val r1 = t1.compute()
      val r2 = t2.join()

      r1 + r2
    }
    else {
      var sum = 0L
      var i = from
      while (i < to) {
        sum += i
        i += 1
      }
      sum
    }
  }
}


class FindTask(what: Long, from: Long, to: Long) extends RecursiveTask[Boolean] {
  def compute(): Boolean = {
    if (to - from > 10000) {
      val t1 = new FindTask(what, from, (to + from) / 2)
      val t2 = new FindTask(what, (to + from) / 2, to)

      t2.fork()

      val r1 = t1.compute()
      val r2 = t2.join()

      r1 || r2
    }
    else {
      for (i <- from until to) {
        if (what == i)
          return true
      }
      false
    }
  }
}