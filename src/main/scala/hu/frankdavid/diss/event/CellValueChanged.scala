package hu.frankdavid.diss.event

import hu.frankdavid.diss.expression.{Value, Cell}

case class CellValueChanged(cell: Cell, value: Value)
