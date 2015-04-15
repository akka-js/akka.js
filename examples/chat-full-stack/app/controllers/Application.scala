package controllers

import scala.concurrent.Await
import scala.language.postfixOps

import scala.concurrent.duration._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka

import akka.actor._
import akka.pattern.ask
import akka.scalajs.wsserver.{ServerProxyActor, ActorWebSocket}
//import akka.scala
import actors._


object Application extends Controller {

  import play.api.Play.current

  implicit val timeout = akka.util.Timeout(5 seconds)

  implicit def ec = Akka.system.dispatcher

  val chatManager = Akka.system.actorOf(Props[ChatManager], name = "chat")

  def indexDev = Action {
    Ok(views.html.index(devMode = true))
  }

  def indexOpt = Action {
    Ok(views.html.index(devMode = false))
  }

  def chatWSEntry = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    val f = chatManager ? NewConnection(out)

    Await.result(f, 5 seconds).asInstanceOf[Props]
  }

}
