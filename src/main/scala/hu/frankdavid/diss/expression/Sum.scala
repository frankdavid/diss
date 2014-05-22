package hu.frankdavid.diss.expression

import hu.frankdavid.diss.DataTable
import hu.frankdavid.diss.types.{CellRange, Matrix, MatrixLike}
import scala.collection.mutable.ListBuffer

case class Sum(override val parameters: Array[HasValue]) extends Expression {
  def evaluate(implicit table: DataTable) = {
    val ops = operands(table)

    var sum: Any = clone(ops(0))
    var i = 1
    while (i < ops.length) {
      sum = genericSum(sum, ops(i))
      i += 1
    }
    sum

  }

  def sumMatrices(addTo: Matrix, b: MatrixLike): Matrix = {
    if (b.cols != addTo.cols || b.rows != addTo.rows)
      throw new IllegalArgumentException("Parameter matrix has to be of same size")
    var r, c = 0
    while (r < addTo.rows) {
      c = 0
      while (c < addTo.cols) {
        addTo(r, c) = genericSum(addTo(r, c), b(r, c))
        c += 1
      }
      r += 1
    }
    addTo
  }

  def clone(obj: Any): Any = {
    obj match {
      case m: MatrixLike => m.clone()
      case _ => obj
    }
  }

  def genericSum(addTo: Any, b: Any): Any = {
    addTo match {
      case number: Long =>
        b match {
          case number2: Long => number + number2
          case number2: Int => number + number2
          case _ => null
        }
      case number: Int =>
        b match {
          case number2: Long => number + number2
          case number2: Int => number + number2
          case _ => null
        }
      case matrix: Matrix =>
        b match {
          case matrix2: MatrixLike => sumMatrices(matrix, matrix2)
          case _ => null
        }
      case _ => null
    }

  }

  override def mapReduce(maxCost: Int)(implicit table: DataTable) = {
    if (parameters.length == 1)
      parameters(0) match {
        case Const(range: CellRange) => {
          val sqr = math.round(Math.sqrt(maxCost)).toInt
          val parts = new ListBuffer[Expression]
          var row = 0
          var col = 0

          while (row < range.from.row) {
            val rowTo = math.min(range.to.row, row + sqr)
            col = 0
            while (col < range.from.col) {
              val colTo = math.min(range.to.col, col + sqr)
              parts += Sum(Array(Const(new CellRange(Cell(row, col), Cell(rowTo, colTo)))))
              col = colTo
            }
            row = rowTo
          }
          Some(MapReduce(parts.toList, Sum(parts.toArray)))
        }
        case _ => None
      }
    else
      None

  }

  def cost(implicit table: DataTable) = {
    if (parameters.length == 1)
      parameters(0) match {
        case Const(range: CellRange) => range.cols * range.rows
        case _ => 1
      }
    else 1
  }
}