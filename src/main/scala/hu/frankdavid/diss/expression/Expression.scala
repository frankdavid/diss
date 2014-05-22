package hu.frankdavid.diss.expression

import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.DataTable.UpdateResult

trait Expression extends HasValue with HasDependencies {
  def parameters: Array[HasValue] = Array.empty
  def evaluate(implicit table: DataTable): Any
  def mapReduce(maxCost: Int)(implicit table: DataTable): Option[MapReduce] = None
  def cost(implicit table: DataTable): Long
  def evaluateAndSave(implicit table: DataTable): UpdateResult = {
    val result = evaluate(table)
    table.put(this, result)
  }
  def operands(implicit table: DataTable) = {
    parameters.map(table.get(_).orNull)
  }
}

