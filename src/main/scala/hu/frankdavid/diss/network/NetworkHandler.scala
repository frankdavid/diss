package hu.frankdavid.diss.network

import com.esotericsoftware.kryonet.{Listener, Connection, Client, Server}

import scala.collection.JavaConversions._
import java.net.{InetAddress, NetworkInterface, InetSocketAddress}
import scala.collection.mutable
import scala.util.Random

class NetworkHandler {
  val ServerTcpPort = 9876
  val ServerUdpPort = 9877

  val TimeoutMillis = 3000
  val HeartBeatFrequency = 400

  val server = new Server()

  val peers = new mutable.HashMap[InetAddress, Peer]()

  def createClient() = {
    val client = new Client()
    client.start()
    client.getKryo.register(classOf[HeartBeat])
    client.addListener(PeerListener)
    client
  }

  /**
   * Starts a client and server and connects to all discovered server on the local network
   */
  def init() {
    ServerThread.start()
    val discoverClient = createClient()

    val hosts = discoverClient.discoverHosts(ServerUdpPort, TimeoutMillis)
    discoverClient.close()
    for (host <- hosts) {
      if (!NetworkInterface.getNetworkInterfaces.exists(_.getInetAddresses.exists(_ == host))) {
        val client = createClient()
        client.connect(100, host, ServerTcpPort, ServerUdpPort)
        peers(host) = new Peer(Right(client))
      }
    }

    SendHeartBeatThread.start()
  }

  def sendToAll(obj: Any) {
    for(peer <- peers) {
      peer._2.connection.fold(_.sendTCP(obj), _.sendTCP(obj))
    }
  }

  object ServerListener extends Listener {
    override def connected(connection: Connection) {
      peers(connection.getRemoteAddressTCP.getAddress) = new Peer(Left(connection))
    }
  }

  object PeerListener extends Listener {
    override def received(connection: Connection, message: scala.Any) {
      message match {
        case HeartBeat(capacity: Int) => {
          peers(connection.getRemoteAddressTCP.getAddress).capacity = capacity
          println(connection.getRemoteAddressTCP + " capacity is: " + capacity)
        }
        case _ =>
      }
    }

    override def disconnected(connection: Connection) {
      peers.remove(connection.getRemoteAddressTCP.getAddress)
    }
  }


  object SendHeartBeatThread extends Thread {
    val random = new Random()

    override def run() {
      while (true) {
        sendToAll(HeartBeat(random.nextInt(5)))
        Thread.sleep(HeartBeatFrequency)
      }
    }
  }

  object ServerThread extends Thread {
    override def run() {
      server.start()
      server.getKryo.register(classOf[HeartBeat])
      server.bind(ServerTcpPort, ServerUdpPort)
      server.addListener(ServerListener)
      server.addListener(PeerListener)
    }
  }

}

object NetworkHandlerTest extends App {
  new NetworkHandler().init()
}
