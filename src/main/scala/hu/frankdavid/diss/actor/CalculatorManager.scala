package hu.frankdavid.diss.actor

import hu.frankdavid.diss.expression._
import scala.collection.mutable
import hu.frankdavid.diss.util.JobList
import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.DataTable.UpdateResult
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.network.NetworkHandler
import scala.concurrent.{Promise, ExecutionContext, Future, future}
import ExecutionContext.Implicits.global


class CalculatorManager(socket: WebSocketHandler, table: DataTable, network: NetworkHandler) {

  socket.calculatorManager = this
  network.calculatorManager = this

  private var jobs = new JobList()

  private var jobsRunning: Future[Unit] = null

  private var promises = mutable.HashMap[Expression, Promise[Any]]()

  private val expressionsUnderProgress = new mutable.HashSet[Expression]()

  var load = 0


  def calculate(expression: Expression): Future[Any] = {
    synchronized {
      jobs += expression
      val p = Promise[Any]()
      promises(expression) = p
      issueJobs()
      p.future
    }
  }

  def get(expression: HasValue) = table.get(expression)

  def getAllCells() = {
    table.bindings.map {
      case (cell, hasValue) => (cell, (hasValue, table.get(hasValue)))
    }.toList
  }

  def put(expression: Expression, value: Any, processUpdate: Boolean = true) {
    val result = table.put(expression, value)
    processUpdateResult(result)
  }

  def bind(cell: Cell, expression: HasValue, processUpdate: Boolean = true) {
    socket.cellBindingChanged(cell, expression)
    val result = table.bind(cell, expression)
    if (processUpdate) {
      network.sendBindingChanged(cell, expression)
      processUpdateResult(result)
      issueJobs()
    }
  }

  def mergeJobs(targetCost: Long) {
    var changed = false
    do {
      changed = false
      jobs.foreach {
        job =>
          if (job.cost(table) < targetCost && job.parameters.length == 1) {
            job.parameters(0) match {
              case parent: Expression =>
                jobs.remove(parent)
                jobs.remove(job)
                jobs.add(new CompoundExpression(List(parent, job)))
                changed = true
              case _ =>
            }
          }
      }
    } while (changed)
  }

  def parametersReady(expression: Expression): Boolean = {
    for (p <- expression.parameters) {
      p match {
        case e: Expression if expressionsUnderProgress.contains(e) => return false
        case _ =>
      }
    }
    true
  }

  def issueJobs() {
    if (jobsRunning == null) {
      jobsRunning = future {
        issueJobsSync()
      }
      jobsRunning.onComplete {
        case _ =>
          jobsRunning = null
      }
    }
  }

  private def issueJobsSync() {
    while (this.jobs.size > 0) {
      val jobs = this.jobs
      this.jobs = new JobList()

      mergeJobs(200)
      sortJobsTopologically()
      jobs.foreach {
        job =>
          if (parametersReady(job)) {
            jobs.remove(job)
            if (job.cost(table) > 2000) {
              evaluateExpressionAsync(job)
            } else {
              val result = job.evaluateAndSave(table)
              processUpdateResult(result)
            }
          }
      }
      this.jobs ++= jobs
    }
    resolvePromises()
  }

  private def evaluateExpressionAsync(expression: Expression) {
    expressionsUnderProgress += expression
    future {
      val result = expression.evaluateAndSave(table)
      processUpdateResult(result)
    }.andThen {
      case _ => expressionsUnderProgress -= expression
    }
  }

  private def resolvePromises() {
    val promises = this.promises
    this.promises = new mutable.HashMap[Expression, Promise[Any]]()
    promises.foreach {
      case (expression, promise) => future {
        promise.success(table.get(expression).orNull)
      }
    }
  }


  def processUpdateResult(updateResult: UpdateResult) {
    updateResult.notifiedExpressions.foreach {
      case c: Cell => table.get(c).foreach {
        value =>
          socket.cellValueChanged(c, value)
      }
      case expr =>
        table.resolveExpression(expr).map(calculate)
    }
  }

  def sortJobsTopologically() {
    val visited = new mutable.HashSet[HasValue]()
    val visiting = new mutable.HashSet[HasValue]()
    val sorted = new JobList()
    def visit(expression: HasValue) {
      visiting += expression
      table.listeners.getOrElse(expression, Set()).foreach {
        e =>
          if (e != expression && !visited.contains(e)) {
            if (visiting.contains(e))
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