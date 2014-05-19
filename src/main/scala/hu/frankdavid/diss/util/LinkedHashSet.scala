package hu.frankdavid.diss.util

import scala.collection.{Iterator, mutable}

class LinkedHashSet[A] extends mutable.LinkedHashSet[A] {
  def reverseIterator: Iterator[A] = new Iterator[A] {
    private var cur = lastEntry
    def hasNext = cur ne null
    def next =
      if (hasNext) { val res = cur.key; cur = cur.earlier; res }
      else Iterator.empty.next
  }
}
