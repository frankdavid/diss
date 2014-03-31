package hu.frankdavid.diss.expression

trait HasDependencies extends HasValue {
  def dependencies: Seq[HasValue]
}
