package hu.frankdavid.diss.expression

case class Sum(override val dependencies: Seq[HasValue]) extends Expression {
  def evaluate(params: Seq[Value]) = params.reduce[Value] {
    case (Value(p1: Int), Value(p2: Int)) => Value(p1 + p2) //TODO: it should work for other types
  }
}