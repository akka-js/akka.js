package eu.unicredit

import akka.actor._
import com.typesafe.config.Config
import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

import org.scalajs.dom.raw._

object ChatUI {

  def start(config: Config) = {
    implicit  val system = ActorSystem("streams", config)
    system.actorOf(Props(ChatUI()), "page")
  }

  case class ChatUI()(implicit system: ActorSystem) extends DomActor {
    override val domElement = Some(getElem("root"))

    val urlBox = input("placeholder".attr := "enter url here").render

    def template() = div(cls := "pure-g")(
      div(cls := "pure-u-1-3")(
        h2("Add chat server:"),
        div(cls := "pure-form")(
          button(
            cls := "pure-button pure-button-primary",
            onclick := {
              () => {
                Streams.complexFlow
          }})("Run")
        )
      )
    )
  }



}
