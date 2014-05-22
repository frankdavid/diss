package hu.frankdavid.diss.network

import com.esotericsoftware.kryonet.{Listener, Connection, Client, Server}

import scala.collection.JavaConversions._
import java.net.{InetAddress, NetworkInterface}
import scala.collection.mutable
import scala.util.{Failure, Success, Random}
import hu.frankdavid.diss.expression._
import com.esotericsoftware.kryo.{Serializer, Kryo}
import hu.frankdavid.diss.expression.Cell
import hu.frankdavid.diss.expression.Sum
import hu.frankdavid.diss.expression.Expression
import hu.frankdavid.diss.types.Matrix
import com.esotericsoftware.kryo.io.{Output, Input}
import hu.frankdavid.diss.actor.CalculatorManager
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.future

class NetworkHandler {

  val ServerTcpPort = 9876
  val ServerUdpPort = 9877

  val TimeoutMillis = 3000
  val HeartBeatFrequency = 400

  val server = new Server(1 << 24, 1 << 24)

  val peers = new mutable.HashMap[InetAddress, Peer]()

  var calculatorManager: CalculatorManager = _

  private def createClient() = {
    val client = new Client(1 << 24, 1 << 24)
    client.start()
    registerKryoClasses(client.getKryo)
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
        peers(host) = new Peer(Right(client))
        future {
          client.connect(100, host, ServerTcpPort, ServerUdpPort)
        }
      }
    }

    SendHeartBeatThread.start()
  }


  def sendToAll(obj: Any) {
    for (peer <- peers) {
      sendToPeer(peer._2, obj)
    }
  }

  def sendToPeer(peer: Peer, obj: Any) {
    peer.connection.fold(_.sendTCP(obj), _.sendTCP(obj))
  }

  def sendCalculation(expression: Expression) {
    sendToPeer(peers.maxBy(_._2.capacity)._2, expression)
  }

  def sendBindingChanged(cell: Cell, value: HasValue) = {
    sendToAll(BindingChanged(cell, value))
  }

  def registerKryoClasses(kryo: Kryo) {
    //    kryo.register(classOf[mutable.WrappedArray.ofRef[AnyRef]], new Serializer[mutable.WrappedArray.ofRef[AnyRef]]() {
    //      def write(kryo: Kryo, output: Output, obj: ofRef[AnyRef]) = kryo.writeObject(output, obj.array)
    //
    //      def read(kryo: Kryo, input: Input, p3: Class[ofRef[AnyRef]]) = new ofRef[AnyRef](kryo.readObject(input, classOf[Array[AnyRef]]))
    //    })
    kryo.register(classOf[Sum], new Serializer[Sum] {
      def write(kryo: Kryo, output: Output, p3: Sum) = kryo.writeObject(output, p3.parameters.toArray)

      def read(kryo: Kryo, input: Input, p3: Class[Sum]) = Sum(kryo.readObject(input, classOf[Array[HasValue]]))
    })
    kryo.register(classOf[Matrix], new Serializer[Matrix] {
      def write(kryo: Kryo, output: Output, p3: Matrix) = {
        kryo.writeObject(output, p3.rows)
        kryo.writeObject(output, p3.cols)
        kryo.writeObject(output, p3.fields)
      }

      def read(kryo: Kryo, input: Input, p3: Class[Matrix]) = {
        val rows = kryo.readObject(input, classOf[Int])
        val cols = kryo.readObject(input, classOf[Int])
        val fields = kryo.readObject(input, classOf[Array[Any]])
        new Matrix(rows, cols, fields)
      }
    })
    kryo.register(classOf[Array[HasValue]])
    kryo.register(classOf[HasValue])
    kryo.register(classOf[HeartBeat])
    kryo.register(classOf[BindingChanged])
    kryo.register(classOf[Multiply])
    kryo.register(classOf[Cell])
    kryo.register(classOf[Const])
    kryo.register(classOf[Array[Array[Any]]])
    kryo.register(classOf[Array[Any]])
    kryo.register(classOf[ValueChanged])
    kryo.register(classOf[CountTo])
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
        }
        case ValueChanged(expr, value) =>
          calculatorManager.put(expr, value)
        case BindingChanged(cell, expression) =>
          calculatorManager.bind(cell, expression)
        case e: Expression =>
          println("Expression received")
          calculatorManager.calculate(e).onComplete {
            case Success(res) =>
              println("Sending result")
              connection.sendTCP(ValueChanged(e, res))
            case Failure(throwable) => throwable.printStackTrace()
          }
        case _ =>
      }
    }

    override def disconnected(connection: Connection) {
      peers.find(_._2.connection.left.exists(_ == connection)).foreach(peers -= _._1)
    }
  }


  object SendHeartBeatThread extends Thread {
    val random = new Random()

    override def run() {
      while (true) {
//        sendToAll(HeartBeat(random.nextInt(5)))
        Thread.sleep(HeartBeatFrequency)
      }
    }
  }

  object ServerThread extends Thread {
    override def run() {
      server.start()
      registerKryoClasses(server.getKryo)
      server.bind(ServerTcpPort, ServerUdpPort)
      server.addListener(ServerListener)
      server.addListener(PeerListener)
    }
  }

}

object NetworkHandlerTest extends App {
  new NetworkHandler().init()
}
