package hu.frankdavid.diss.server

import org.mashupbots.socko.routes._
import akka.actor.{ActorRef, ActorSystem, Props}
import hu.frankdavid.diss.ChatHandler
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.webserver.{WebServerConfig, WebServer}
import hu.frankdavid.diss.actor.{WebSocketHandler, WebSocketActor}
import hu.frankdavid.diss.actor.WebSocketActor.{PushAllCells, ReceiveCellBindingChanged}
import scala.concurrent.duration._

object WebSocketServer extends {
  def create(actorSystem: ActorSystem, socket: WebSocketHandler) = {
    import actorSystem.dispatcher
    val routes = Routes({

      case HttpRequest(httpRequest) => httpRequest match {
        case GET(Path("/html")) => {
          // Return HTML page to establish web socket
          httpRequest.response.write("Hello", "text/html; charset=UTF-8")
        }
        case Path("/favicon.ico") => {
          // If favicon.ico, just return a 404 because we don't have that file
          httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
        }
      }

      case WebSocketHandshake(wsHandshake) =>
        wsHandshake.authorize(onComplete = Some((s) => {println("CONNECTED")}))
        wsHandshake match {
          case Path("/websocket/") => {
            wsHandshake.authorize()
            actorSystem.scheduler.scheduleOnce(50 milliseconds) {
              socket.pushAllCells()
            }
          }
        }

      case WebSocketFrame(wsFrame) => {
        socket.receiveCellBindingChanged(wsFrame.readText())
      }

    })

    val server = new WebServer(WebServerConfig(), routes, actorSystem)
    socket.server = server
    server
  }
}
