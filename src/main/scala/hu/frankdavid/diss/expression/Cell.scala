package hu.frankdavid.diss.expression

case class Cell(row: Int, col: Int) extends HasValue {
  def this() = this(0, 0)
}