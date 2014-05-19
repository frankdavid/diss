package hu.frankdavid.diss.expression

case class Value(value: Any) {
  require(!value.isInstanceOf[Value])
  require(value != null)
}