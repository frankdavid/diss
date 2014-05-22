package hu.frankdavid.diss.expression

import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.types.{MatrixLike, Matrix}

case class CountTo(override val parameters: Array[HasValue]) extends Expression {
  def this() = this(null)
  def evaluate(implicit table: DataTable) = {
    val to = operands(table)(0).asInstanceOf[Long]
    var i = 0L
    while(i < to)
      i += 1
    0
  }


  def mapReduce(maxCost: Int) = None

  def cost(implicit table: DataTable) = operands(table)(0).toString.toLong
}