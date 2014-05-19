class Table
  SOCKET_URL: 'ws://localhost:8888/websocket/'
  (@$container) ->

  width: 0
  height: 0
  $trs: []

  cells: {}

  initialize: !(rows, cols)->
    @socket = new WebSocket(@SOCKET_URL)
    @socket.onmessage = !(message) ~>
      @handleMessages($.parseJSON(message.data))
    @$table = $ '<table class="spreadsheet"/>'
    @$container.append(@$table)
    @ensureSize(rows, cols)

  handleMessages: !(messages) ->
    console.log 'messages'
    for message in messages
#      window.setTimeout (message)~>
        if message.value?
          @setValue message.cell[0], message.cell[1], message.value
        if message.binding?
          @setBinding message.cell[0], message.cell[1], message.binding
#      , 0, message
#    @redraw()

  redraw: ->
    table = ""
    for row from 0 til @height
      table += "<tr>"
      for col from 0 til @width
        table += "<td>" + @cell(row, col).value + "</td>"
      table += "</tr>"
    @$table.html(table)


  ensureSize: !(minHeight, minWidth) ->
    if(@width < minWidth)
      for row from 0 til @height
        $row = @$trs[row]
        width = @width
        while width < minWidth
          c = new Cell(row, width, @socket)
          @setCell(row, width, c)
          $row.append(c.$elem())
          width++
      @width = Math.max @width, minWidth
    while @height < minHeight
      $tr = $("<tr></tr>")
      for col from 0 til @width
        c = new Cell(@height, col, @socket)
        @setCell(@height, col, c)
        $tr.append c.$elem()
      @$table.append($tr)
      @$trs.push($tr)
      @height++

  cell: (row, col) ->
    @cells["#row,#col"]

  setCell: !(row, col, cell) ->
    @cells["#row,#col"] = cell



  setValue: !(row, column, value) ->
    @ensureSize(row + 1, column + 1)
    @cell(row, column).setValue value

  setBinding: !(row, column, value) ->
    @ensureSize(row + 1, column + 1)
    @cell(row, column).setBinding value


class Cell
  (@row, @coll, @socket) ->
    @$td = $ '<td><div class="value"></div><input style="display:none" class="binding"/></td>'
    @$value = @$td.find('.value').eq(0)
    @$binding = @$td.find('.binding').eq(0)
    @$td.dblclick !~>
      @$value.hide()
      @$binding.show().focus()
    @$binding.blur ~> @blur()
    @$binding.keypress (event) ~>
      if event.keyCode == 13
        @blur()

  blur: !->
    @$binding.hide()
    @$value.show()
    @setBinding @$binding.val()
    @sendToServer()

  $elem: -> @$td

  sendToServer: !->
    @socket.send JSON.stringify {
        cell: [@row, @coll]
        binding: @$binding.val()
    }

  setBinding: !(binding) ->
    @binding = binding
    @update()

  update: !->
    if @binding?.substr(0, 1) == '='
      @$value.html @value
    else
      @$value.html @binding

    @$binding.val @binding


  setValue: !(value) ->
    @value = value
    @update()






$(document).ready !->
  table = new Table($ '#table-container')
  table.initialize(30, 10)