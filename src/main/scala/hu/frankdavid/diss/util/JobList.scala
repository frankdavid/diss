package hu.frankdavid.diss.util

import scala.collection.{Iterator, mutable}
import hu.frankdavid.diss.expression.Expression

class JobList extends mutable.LinkedHashSet[Expression] {
  def reverseIterator: Iterator[Expression] = new Iterator[Expression] {
    private var cur = lastEntry
    def hasNext = cur ne null
    def next =
      if (hasNext) { val res = cur.key; cur = cur.earlier; res }
      else Iterator.empty.next
  }
}
