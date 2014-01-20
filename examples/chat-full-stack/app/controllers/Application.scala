package controllers

import play.api._
import play.api.mvc._

import akka.actor.Props
import akka.scalajs.wsserver.ActorWebSocket
import actors._

object Application extends Controller {

  import play.api.Play.current

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def chatWSEntry = ActorWebSocket(Props[ChatEntryPoint])

}
