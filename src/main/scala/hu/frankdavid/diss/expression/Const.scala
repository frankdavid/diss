package hu.frankdavid.diss.expression

case class Const(value: Any) extends HasValue {
  def this() = this(null)
}

object Const {
//  def apply(value: Any): Const = Const(Value(value))
}