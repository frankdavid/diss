package hu.frankdavid.diss

import scala.collection.{SetLike, mutable}
import hu.frankdavid.diss.DataTable.{UpdateResult, CacheItem}
import hu.frankdavid.diss.expression._
import hu.frankdavid.diss.expression.Const
import hu.frankdavid.diss.DataTable.CacheItem
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.DataTable.UpdateResult
import scala.Some

class DataTable {
  private val valueCache = new mutable.HashMap[Expression, Value]
  private val listeners = new mutable.HashMap[HasValue, mutable.Set[HasValue]] with mutable.MultiMap[HasValue, HasValue]
  val bindings = new mutable.HashMap[Cell, HasValue]

  def get(expression: HasValue): Option[Value] = {
    expression match {
      case Undefined => None
      case Const(x) => Some(x)
      case null => None
      case cell: Cell =>
        bindings.get(cell).map(get).flatten
      case expr: Expression =>
        subscribeToListeners(expr)
        valueCache.get(expr)
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

  def put(expression: Expression, value: Value): UpdateResult = {
    require(value != null, "Null values cannot be cached")
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
    expression.dependencies foreach {
      case parameter: Const =>
      case parameter => listeners.addBinding(parameter, expression)
    }
  }

  private def unsubscribeFromListeners(expression: HasDependencies) {
//    expression.dependencies foreach {
//      case parameter: Const =>
//      case parameter => listeners.removeBinding(parameter, expression)
//    }
    listeners map {
      case (a, set) if set.contains(expression) => listeners.removeBinding(a, expression) // TODO: maybe add a reverse mapping
    }
  }

}

object DataTable {

  case class UpdateResult(notifiedExpressions: Set[HasValue] = Set.empty) {
  }

  private case class CacheItem(value: Any, listeners: Set[Expression])

//  private case class CellBinding(cell: Cell, value: Expression) extends Expression {
//    def evaluate(params: Seq[Any]) = params(0)
//
//    override def dependencies = Seq(value)
//
//    override def equals(obj: Any) =
//      obj match {
//        case CellBinding(cell2, _) => cell == cell2
//        case _ => false
//      }
//
//    override def hashCode() = cell.hashCode()
//  }

}

