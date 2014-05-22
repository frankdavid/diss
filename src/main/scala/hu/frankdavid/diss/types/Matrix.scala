package hu.frankdavid.diss.types

import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.DataTable

trait MatrixLike extends Iterable[Any] {
  def apply(row: Int, col: Int): Any

  def rows: Int

  def cols: Int


  override def toString() = {
    val builder = new StringBuilder()
    var r, c = 0
    while (r < rows) {
      c = 0
      while (c < cols) {
        builder.append(this(r, c)).append("\\t")
        c += 1
      }
      builder.append("\\n")
      r += 1
    }
    builder.toString()
  }

  def iterator = new Iterator[Any] {
    private var currRow = 0
    private var currCol = -1

    def hasNext = {
      if (currRow == rows - 1)
        currCol < cols - 1
      else
        currCol < cols
    }

    def next() = {
      currCol += 1
      if (currCol >= cols) {
        currRow += 1
        currCol = 0
      }
      apply(currRow, currCol)
    }
  }

  override def clone(): MatrixLike = {
    val result = new Matrix(rows, cols)
    var r, c = 0
    while (r < rows) {
      c = 0
      while (c < cols) {
        result(r, c) = this(r, c)
        c += 1
      }
      r += 1
    }
    result
  }

}

class CellRange(val from: Cell, val to: Cell) extends MatrixLike {
  def apply(row: Int, col: Int) = Cell(from.row + row, from.col + col)

  def cols = to.col - from.col

  def rows = to.row - from.row
}

class Matrix extends MatrixLike {
  var fields = new Array[Any](0)
  var cols = 0
  var rows = 0

  def this(f: Array[Any]*) = {
    this()
    rows = f.length
    if (rows > 0)
      cols = f(0).length
    fields = new Array(cols * rows)
    var r, c = 0
    while (r < rows) {
      c = 0
      while (c < cols) {
        fields(r * cols + c) = f(r)(c)
        c += 1
      }
      r += 1
    }
  }

  def this(rows: Int, cols: Int) = {
    this()
    fields = new Array(cols * rows)
    this.rows = rows
    this.cols = cols
  }

  def this(rows: Int, cols: Int, fields: Array[Any]) {
    this()
    this.rows = rows
    this.cols = cols
    this.fields = fields
  }

  def apply(row: Int, col: Int) = fields(row * cols + col)

  def update(row: Int, col: Int, value: Any) = fields(row * cols + col) = value

}