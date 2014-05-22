package hu.frankdavid.diss.expression

trait HasDependencies extends HasValue {
  def parameters: Array[HasValue]
}
