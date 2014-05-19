package hu.frankdavid.diss.network_test

import java.net._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import java.nio.ByteBuffer
import java.io.File
import com.esotericsoftware.kryonet.{Client, Connection, Listener, Server}

object KryoTest extends App {

  val isServer = args.length > 1
  if (isServer) {
    println("server mode activated")
    val server = new Server()
    server.start()
    server.addListener(new Listener() {
      override def received(connection: Connection, p2: scala.Any) {
        connection.sendTCP(p2)
      }
    })
    server.bind(9876, 9877)
  }

  else {
    println("client mode activated")
    val client = new Client()
    client.start()
    val host = client.discoverHost(9877, 1000)
    client.connect(1000, host, 9876, 9877)
    //    for (i <- 0 to 100) {
    client.updateReturnTripTime()
    Thread.sleep(2000)
    println(client.getReturnTripTime)
    //      Thread.sleep(500)
    //    }
  }

  def sendPing(socket: Socket) {

  }

  def sendPong(socket: Socket) {

  }
}
