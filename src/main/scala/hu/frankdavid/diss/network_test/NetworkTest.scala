package hu.frankdavid.diss.network_test

import java.net._
import scala.concurrent.{ExecutionContext, future}
import ExecutionContext.Implicits.global
import java.nio.ByteBuffer
import java.io.File

object NetworkTest extends App {

  val isServer = new File("server").exists()
  if(isServer) {
    println("server mode activated")
    val receivingSocket = new DatagramSocket(9876)
    while(true) {
      val receivedPacket = new DatagramPacket(new Array[Byte](1000000), 1000000)
      println("received")
      receivingSocket.receive(receivedPacket)
      val answer = new DatagramPacket(receivedPacket.getData, receivedPacket.getLength, receivedPacket.getSocketAddress)
      receivingSocket.send(answer)
    }
  }
  
  else {
    println("client mode activated")
    for (i <- 0 to 100) {
      val socket = new DatagramSocket(900)
      socket.setBroadcast(true)
      //    socket.connect(InetAddress.getByName("255.255.255.255"))
      val answer = new DatagramPacket(new Array[Byte](1000000), 1000000)
      val sendPacket = new DatagramPacket(ByteBuffer.allocate(1000000).putLong(0).array(), 1000000, InetAddress.getByName("255.255.255.255"), 9876)
      socket.send(sendPacket)
      val sentAt = System.nanoTime
      socket.receive(answer)
      val receivedAt = System.nanoTime
      val packetSentAt = ByteBuffer.wrap(answer.getData).getLong
      assert(packetSentAt == 0)
      println((receivedAt - sentAt) / 1e6)
      Thread.sleep(1000)
      socket.close()
    }
  }

  def sendPing(socket: Socket) {

  }

  def sendPong(socket: Socket) {

  }
}
