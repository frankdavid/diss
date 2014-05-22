package hu.frankdavid.diss.network

import hu.frankdavid.diss.expression.{Expression, HasValue}

case class ValueChanged(expression: Expression, value: Any) {
//  def this(expression: Expression, value: Any) = this(expression, value)
  def this() = this(null, null)
}
