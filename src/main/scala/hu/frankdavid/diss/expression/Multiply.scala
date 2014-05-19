package hu.frankdavid.diss.expression

import hu.frankdavid.diss.types.Matrix

case class Multiply(override val dependencies: Seq[HasValue]) extends Expression {
  def evaluate(params: Seq[Value]) = params.reduce[Value] {
    case (Value(m1: Matrix), Value(m2: Matrix)) => Value(m1) //TODO: it should work for other types
  }

  def mapReduce(maxCost: Int) = {
    MapReduce(null, null)
  }

  def cost = 1

}