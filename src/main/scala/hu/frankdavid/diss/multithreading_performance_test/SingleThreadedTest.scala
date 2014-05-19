package hu.frankdavid.diss.multithreading_performance_test

object SingleThreadedTest extends PerformanceTest {
  def runTest() {
    var sum = 0L
    var i = 0L
    while (i < 10000000000L) {
      sum += i
      i += 1
    }
    println(sum)
  }
}
