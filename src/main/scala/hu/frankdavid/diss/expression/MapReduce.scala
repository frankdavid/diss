package hu.frankdavid.diss.expression

case class MapReduce(forks: Seq[Expression], reduce: Expression)
