package hu.frankdavid.diss.expression

case class Const(value: Value) extends HasValue

object Const {
  def apply(value: Any): Const = Const(Value(value))
}