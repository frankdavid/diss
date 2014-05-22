package hu.frankdavid.diss.expression

import hu.frankdavid.diss.DataTable

object Undefined extends Expression {
  def evaluate(implicit table: DataTable): Any = null
  def mapReduce(maxCost: Int) = None
  def cost(implicit table: DataTable) = 0
}
