package hu.frankdavid.diss.expression

trait Expression extends HasValue with HasDependencies {
  def dependencies: Seq[HasValue] = Seq.empty
  def evaluate(params: Seq[Value]): Value
  def mapReduce(maxCost: Int): MapReduce
  def cost: Long
}

