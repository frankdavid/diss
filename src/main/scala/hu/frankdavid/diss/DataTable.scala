package hu.frankdavid.diss

import scala.collection.mutable
import hu.frankdavid.diss.DataTable.UpdateResult
import hu.frankdavid.diss.expression._
import hu.frankdavid.diss.expression.Const
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.DataTable.UpdateResult
import scala.Some

class DataTable {

  private val valueCache = new mutable.HashMap[Expression, Any]
  val listeners = new mutable.HashMap[HasValue, mutable.Set[HasValue]] with mutable.MultiMap[HasValue, HasValue]
  val bindings = new mutable.HashMap[Cell, HasValue]

  def invalidate(expression: Expression) {
      valueCache -= expression
  }

  def get(expression: HasValue): Option[Any] = {
    expression match {
      case Undefined => None
      case Const(x) => Option(x)
      case null => None
      case cell: Cell =>
        bindings.get(cell).map(get).flatten
      case expr: Expression =>
        subscribeToListeners(expr)
        valueCache.get(expr)
      case _ => None
    }
  }

  def resolveExpression(expression: HasValue): Option[Expression] = {
    expression match {
      case e: Expression => Some(e)
      case c: Cell => bindings.get(c).map(resolveExpression).flatten
      case _ => None
    }
  }

  def bind(cell: Cell, value: HasValue): UpdateResult = {
    val oldBinding = bindings.get(cell)
    if (!oldBinding.exists(_ == value)) { // if old binding doesn't exist or doesn't equal to new value
//      oldBinding match {
//        case Some(expression: HasDependencies) if expression != value =>  unsubscribeFromListeners(expression) // if old binding is an expression, unsubscribe from getting notified
//        case _ =>
//      }
      value match {
        case expression: HasDependencies => subscribeToListeners(expression)
        case _ =>
      }
      bindings(cell) = value
    }
    listeners.addBinding(value, cell)
    val updateResult = value match {
      case expr: Expression if !valueCache.contains(expr) => UpdateResult(extendNotifiedExpressionsWithBoundCells(Set(expr) ++ listeners.getOrElse(cell, mutable.Set.empty)))
      case _ => UpdateResult(extendNotifiedExpressionsWithBoundCells(listeners.getOrElse(cell, mutable.Set.empty)))
    }
    updateResult
  }

  def extendNotifiedExpressionsWithBoundCells(notifiedExpressions: collection.Set[HasValue]): Set[HasValue] = {
    val notified = mutable.Set[HasValue]()
    def extendWith(expression: HasValue) {
      notified += expression
      listeners.get(expression) match {
        case Some(l) => l.collect {
          case c: Cell =>
            val isNew = notified.add(c)
            if(isNew)
              extendWithCell(c)
        }
        case None =>
      }
    }
    def extendWithCell(cell: Cell) {
      notified += cell
      listeners.get(cell) match {
        case Some(l) => l.collect {
          case c: Cell =>
            val isNew = notified.add(c)
            if(isNew)
              extendWithCell(c)
          case e => extendWith(e)
        }
        case None =>
      }
    }
    notifiedExpressions.collect {
      case c: Cell => extendWithCell(c)
      case e => extendWith(e)
    }
    Set() ++ notified
  }

  def put(expression: Expression, value: Any): UpdateResult = {
//    require(value != null, "Null values cannot be cached")
    valueCache.get(expression) match {
      case Some(oldValue) =>
        if (oldValue != value) {
          // if cached value is not equal to new value, update result
          valueCache.put(expression, value)
          UpdateResult(extendNotifiedExpressionsWithBoundCells(listeners.getOrElse(expression, mutable.Set.empty)))
        }
        else
          UpdateResult() // cached value is the same, nothing to do
      case _ => // expression is not yet in the cache
        valueCache.put(expression, value)
        subscribeToListeners(expression)
        UpdateResult(extendNotifiedExpressionsWithBoundCells(listeners.getOrElse(expression, mutable.Set.empty)))
    }
  }

  private def subscribeToListeners(expression: HasDependencies) {
    expression.parameters foreach {
      case parameter: Const =>
      case parameter => listeners.addBinding(parameter, expression)
    }
  }

  private def unsubscribeFromListeners(expression: HasDependencies) {
    listeners map {
      case (a, set) if set.contains(expression) => listeners.removeBinding(a, expression) // TODO: maybe add a reverse mapping
    }
  }

}

object DataTable {

  case class UpdateResult(notifiedExpressions: Set[HasValue] = Set.empty) {
    def +(other: UpdateResult) = UpdateResult(notifiedExpressions ++ other.notifiedExpressions)
  }

  private case class CacheItem(value: Any, listeners: Set[Expression])


}

