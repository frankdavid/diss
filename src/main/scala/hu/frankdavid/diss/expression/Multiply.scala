package hu.frankdavid.diss.expression

import hu.frankdavid.diss.DataTable

case class Multiply(override val parameters: Array[HasValue]) extends Expression {
  def this() = this(null)
  def evaluate(implicit table: DataTable) = {
    table.get(parameters.head)
  }

  def mapReduce(maxCost: Int) = None

  def cost(implicit table: DataTable) = 1
}