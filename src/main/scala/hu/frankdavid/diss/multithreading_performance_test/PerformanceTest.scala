package hu.frankdavid.diss.multithreading_performance_test

abstract class PerformanceTest extends App {
  val start = System.currentTimeMillis()
  runTest()
  println((System.currentTimeMillis() - start) + " millis")

  def runTest(): Unit
}
