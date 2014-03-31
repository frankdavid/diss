package hu.frankdavid.diss.expression

object Undefined extends Expression {
  def evaluate(params: Seq[Value]): Value = throw new Exception("Cannot evaluate Undefined")
}
