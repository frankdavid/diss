package hu.frankdavid.diss.expression

import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.DataTable.UpdateResult
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

class CompoundExpression(expressions: List[Expression]) extends Expression {
  def this() = this(null)

  def evaluate(implicit table: DataTable) = ??? //compound expressions don't have this method


  override val parameters: Array[HasValue] = {
    val params = new mutable.ArrayBuilder.ofRef[HasValue]()
    expressions.foreach({ e =>
      for(p <- e.parameters)
        if(!expressions.contains(p))
          params += p
    })
    params.result()
  }

  override def evaluateAndSave(implicit table: DataTable) = {
    var result = UpdateResult()
    expressions.map {
      expr =>
        result += expr.evaluateAndSave(table)
    }
    result
  }

  override def mapReduce(maxCost: Int)(implicit table: DataTable): Option[MapReduce] = {
    var cost = 0L
    val map = new ListBuffer[Expression]()
    var current = new ListBuffer[Expression]()
    expressions.foreach {
      expr =>
        cost += expr.cost(table)
        if(cost > maxCost) {
          if(current.length == 0)
            map += expr
          else {
            current += expr
            map += new CompoundExpression(current.toList)
          }
          cost = 0
        } else {
          current += expr
        }
    }
    Some(MapReduce(map.toList, Undefined))
  }

  def cost(implicit table: DataTable) = expressions.map(_.cost).sum
}
