package hu.frankdavid.diss.network

import hu.frankdavid.diss.expression.{Cell, HasValue}

case class BindingChanged(cell: Cell, expression: HasValue) {
  def this() = this(null, null)
}
