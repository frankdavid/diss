package hu.frankdavid.diss

import org.scalatest.{FlatSpec, Matchers}
import hu.frankdavid.diss.expression.{Const, Cell, Sum}

class DataTableSpec extends FlatSpec with Matchers {
  it should "return bind result" in {
    val dataTable = new DataTable
    dataTable.bind(Cell(1, 1), Sum(Seq(Const(5), Const(6)))).notifiedExpressions should contain (Cell(1, 1))
  }
  it should "notify simple expressions" in {
    val dataTable = new DataTable
    dataTable.bind(Cell(1, 1), Const(5))
    dataTable.bind(Cell(1, 2), Const(6))
    val sumExpr = Sum(Seq(Cell(1, 1), Cell(1, 2)))
    dataTable.put(sumExpr)
    val result = dataTable.bind(Cell(1, 2), Const(7))

    result.notifiedExpressions should have size 1
    result.notifiedExpressions should contain (sumExpr)
  }

  it should "notify expressions in cells" in {
    val valueCache = new DataTable
    valueCache.bind(Cell(1, 1), Const(5))
    valueCache.bind(Cell(1, 2), Const(6))
    val sumExpr = Sum(Seq(Cell(1, 1), Cell(1, 2)))
    valueCache.bind(Cell(1, 3), sumExpr)
    valueCache.bind(Cell(1, 1), Sum(Seq(Cell(1, 2), Const(3))))

    val result = valueCache.put(Sum(Seq(Cell(1, 2), Const(3))), 5)

    result.notifiedExpressions should contain (Cell(1, 1))
  }

}
