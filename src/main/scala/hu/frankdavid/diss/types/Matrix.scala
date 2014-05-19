package hu.frankdavid.diss.types

import hu.frankdavid.diss.expression.HasValue

trait MatrixLike {
  def get(row: Int, col: Int)
}

class Matrix extends MatrixLike {
  def get(row: Int, col: Int) = ???
}