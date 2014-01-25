package client

import scala.language.postfixOps

import scala.collection.mutable
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.Ask.ask
import akka.scalajs.wsclient._
import akka.event.LoggingReceive
import akka.util.Timeout

import models._

import scala.scalajs.js
import org.scalajs.jquery.{jQuery => jQ, _}

object Main {
  RegisterPicklers.registerPicklers()

  val system = ActorSystem("chat-client")
  val manager = system.actorOf(Props(new Manager))

  private[this] var myFocusedTab: TabInfo = null
  def focusedTab: TabInfo = myFocusedTab
  def focusedTab_=(v: TabInfo): Unit = {
    if (myFocusedTab ne v) {
      if (myFocusedTab ne null)
        myFocusedTab.deactivate()
      myFocusedTab = v
      v.activate()
    }
  }

  val roomsTab = new RoomsTabInfo
  val roomTabInfos = mutable.Map.empty[Room, DiscussionTabInfo]

  def getDiscussionTabInfoOrCreate(room: Room): DiscussionTabInfo = {
    roomTabInfos.getOrElseUpdate(room, new DiscussionTabInfo(room))
  }

  def startup(): Unit = {
    jQ("#connect-button") click { (event: JQueryEventObject) => connect }
    jQ("#disconnect-button") click { (event: JQueryEventObject) => disconnect }
    jQ("#send-button") click { (event: JQueryEventObject) => send }

    roomsTab.focusTab()
  }

  def connect(): Unit = {
    val nickname = jQ("#nickname-edit").value().toString()
    val email = jQ("#email-edit").value().toString()
    val gravatarHash = computeGravatarHash(email)
    val user = User(nickname, gravatarHash)
    manager ! ConnectAs(user)
  }

  def disconnect(): Unit = {
    manager ! Disconnect
  }

  def send(): Unit = {
    val text = jQ("#msg-to-send").value().toString()
    manager ! Send(text)
    jQ("#msg-to-send").value("")
  }

  def joinRoom(room: Room): Unit = {
    roomTabInfos.get(room).fold {
      manager ! Join(room)
    } {
      _.focusTab()
    }
  }

  def startPrivateChat(dest: User): Unit = ()

  def computeGravatarHash(email: String): String =
    js.Dynamic.global.hex_md5(email.trim.toLowerCase).asInstanceOf[js.String]

  def gravatarURL(user: User, size: Int): String =
    s"http://www.gravatar.com/avatar/${user.gravatarHash}?s=$size"
}

object UsersContainer {
  val container = jQ(".conversation-wrap")

  def clear(): Unit = container.empty()

  def addEntry(header: String, imageURL: String = null)(body: JQuery): Unit = {
    val optImageURL = Option(imageURL)
    val entry = jQ("""<div class="media conversation">""")
    optImageURL foreach { url =>
      entry.append(
        jQ("""<div class="pull-left">""").append(
          jQ("""<img class="media-object" alt="gravatar" style="width: 50px; height: 50px">""").attr(
            "src", url
          )
        )
      )
    }
    entry.append(
      jQ("""<div class="media-body">""").append(
        jQ("""<h5 class="media-heading">""").text(header),
        jQ("""<small>""").append(body)
      )
    )
    container.append(entry)
  }
}

object MessagesContainer {
  val container = jQ(".msg-wrap")

  def clear(): Unit = container.empty()

  def addMessage(author: String, text: String, timestamp: js.Date,
      imageURL: String = null): Unit = {
    val timeStampStr = timestamp.toString()
    val optImageURL = Option(imageURL)
    val entry = jQ("""<div class="media msg">""")
    optImageURL foreach { url =>
      entry.append(
        jQ("""<a class="pull-left" href="#">""").append(
          jQ("""<img class="media-object" alt="gravatar" style="width: 32px; height: 32px">""").attr(
            "src", url
          )
        )
      )
    }
    entry.append(
      jQ("""<div class="media-body">""").append(
        jQ(s"""<small class="pull-right time"><i class="fa fa-clock-o"></i> $timeStampStr</small>"""),
        jQ("""<h5 class="media-heading">""").text(author),
        jQ("""<small class="col-lg-10">""").text(text)
      )
    )
    container.append(entry)
  }

  def addMessage(user: User, text: String, timestamp: js.Date): Unit =
    addMessage(user.nick, text, timestamp, Main.gravatarURL(user, 32))

  def addMessage(msg: Message): Unit =
    addMessage(msg.user, msg.text, new js.Date(msg.timestamp))
}

object SendMessageButton {
  val button = jQ("#send-button")
  val messageInput = jQ("#msg-to-send")

  def disabled: Boolean = button.prop("disabled").asInstanceOf[js.Boolean]
  def disabled_=(v: Boolean): Unit = button.prop("disabled", v)

  var target: ActorRef = Main.system.deadLetters

  button click { (e: JQueryEventObject) =>
    sendMessage()
    false
  }
  messageInput keydown { (e: JQueryEventObject) =>
    if (e.which == (13: js.Number)) {
      e.preventDefault()
      sendMessage()
    }
  }

  def sendMessage(): Unit = {
    val message = messageInput.value().toString()
    if (!disabled && message != "") {
      messageInput.value("")
      println(s"$target ! Send($message)")
      target ! Send(message)
    }
  }
}

abstract class TabInfo(val name: String) {
  val tabLi = jQ("""<li>""").appendTo(jQ("#room-tabs"))
  val tabButton = jQ("""<a href="#">""").text(name).appendTo(tabLi)
  tabButton click { (e: JQueryEventObject) => focusTab(); false }

  def isFocused: Boolean = Main.focusedTab eq this

  def focusTab(): Unit = {
    Main.focusedTab = this
  }

  def activate(): Unit = {
    assert(isFocused, s"Trying to activate non-focused tab $name")
    tabLi.addClass("active")
    render()
  }

  def deactivate(): Unit = {
    tabLi.removeClass("active")
  }

  def invalidate(): Unit = {
    if (isFocused)
      render()
  }

  def render(): Unit = {
    import Main._
    UsersContainer.clear()
    MessagesContainer.clear()
  }
}

trait CloseableTab extends TabInfo {
  tabButton.text(tabButton.text() + " ")
  val closeButton = jQ("""<button type="button" class="close" aria-hidden="true">&times;</button>""").appendTo(tabButton)
  closeButton click { (e: JQueryEventObject) => closeTab(); false }

  def closeTab(): Unit = {
    Main.roomsTab.focusTab()
    Main.roomTabInfos.find(_._2 eq this).foreach(Main.roomTabInfos -= _._1)
    tabLi.remove()
  }
}

class RoomsTabInfo extends TabInfo("Rooms") {
  import Main._

  private[this] var myRooms: List[Room] = Nil
  def rooms: List[Room] = myRooms
  def rooms_=(v: List[Room]): Unit = {
    myRooms = v
    invalidate()
  }

  override def render(): Unit = {
    super.render()

    // Abuse the users container to put room list
    for (room <- rooms) {
      UsersContainer.addEntry(room.name) {
        jQ("""<a href="#" role="button"><i class="fa fa-plus"></i> Join</a>""").click {
          (e: JQueryEventObject) => joinRoom(room); false
        }
      }
    }
    UsersContainer.addEntry("New room") {
      (
        jQ("""<input type="text" class="form-control new-room-name">""")
      ).add(
        jQ("""<a href="#" role="button"><i class="fa fa-plus"></i> Join</a>""").click {
          (e: JQueryEventObject) =>
            joinRoom(Room(jQ(".new-room-name").value().toString()))
            false
        }
      )
    }
  }
}

class DiscussionTabInfo(room: Room) extends TabInfo(room.name)
                                       with CloseableTab {

  private[this] var myManager: ActorRef = Main.system.deadLetters
  def manager: ActorRef = myManager
  def manager_=(v: ActorRef): Unit = {
    myManager = v
    if (isFocused)
      SendMessageButton.target = v
  }

  val users = new mutable.ListBuffer[User]
  val messages = new mutable.ListBuffer[Message]

  override def render(): Unit = {
    super.render()
    renderUsers()
    renderMessages()
  }

  def renderUsers(): Unit = {
    import Main._
    for (user <- users) {
      UsersContainer.addEntry(user.nick, gravatarURL(user, 50)) {
        /*jQ("""<a href="#" role="button"><span class="glyphicon glyphicon-user"></span> Private chat</a>""").click {
          (e: JQueryEventObject) => startPrivateChat(user); false
        }*/
        jQ("""<span></span>""")
      }
    }
  }

  def renderMessages(): Unit = {
    import Main._

    for (message <- messages) {
      MessagesContainer.addMessage(message)
    }

    // scroll to latest message
    jQ(".msg-wrap").scrollTop(jQ(".msg-wrap")(0).scrollHeight)
  }

  override def activate(): Unit = {
    super.activate()
    SendMessageButton.target = manager
    SendMessageButton.disabled = false
  }

  override def deactivate(): Unit = {
    super.deactivate()
    SendMessageButton.target = Main.system.deadLetters
    SendMessageButton.disabled = true
  }

  override def closeTab(): Unit = {
    super.closeTab()
    manager ! Leave
  }
}

case class ConnectAs(user: User)
case class Send(text: String)
case object Disconnect
case object Disconnected

class Manager extends Actor {
  val proxyManager = context.actorOf(Props(new ProxyManager))
  var user: User = User.Nobody
  var service: ActorRef = context.system.deadLetters

  def receive = LoggingReceive {
    case m @ ConnectAs(user) =>
      this.user = user
      jQ("#connect-button").text("Connecting ...").prop("disabled", true)
      jQ("#nickname-edit").prop("disabled", true)
      proxyManager ! m

    case m @ WebSocketConnected(entryPoint) =>
      service = entryPoint
      service ! Connect(user)
      jQ("#status-disconnected").addClass("status-hidden")
      jQ("#status-connected").removeClass("status-hidden")
      jQ("#nickname").text(user.nick)
      jQ("#send-button").prop("disabled", false)
      jQ("#disconnect-button").text("Disconnect").prop("disabled", false)

    case RoomListChanged(rooms) =>
      Main.roomsTab.rooms = rooms

    case Send(text) =>
      val message = Message(user, text, System.currentTimeMillis())
      service ! SendMessage(message)

    case ReceiveMessage(message) =>
      Console.err.println(s"receiving message $message")
      addMessage(message)

    case m @ Disconnect =>
      jQ("#disconnect-button").text("Disconnecting ...").prop("disabled", true)
      proxyManager ! m

    case Disconnected =>
      service = context.system.deadLetters
      jQ("#connect-button").text("Connect").prop("disabled", false)
      jQ("#nickname-edit").prop("disabled", false)
      jQ("#send-button").prop("disabled", true)
      jQ("#status-disconnected").removeClass("status-hidden")
      jQ("#status-connected").addClass("status-hidden")
      context.children.filterNot(proxyManager == _).foreach(context.stop(_))

    case m @ Join(room) =>
      context.actorOf(Props(new RoomManager(user, room, service)))
  }

  def addMessage(message: Message) = {
    val timeStampStr = new js.Date(message.timestamp).toString()
    jQ(".msg-wrap").append(
      jQ("""<div class="media msg">""").append(
        jQ("""<a class="pull-left" href="#">""").append(
          jQ("""<img class="media-object" alt="gravatar" style="width: 32px; height: 32px">""").attr(
              "src", s"http://www.gravatar.com/avatar/${message.user.gravatarHash}?s=32")
        ),
        jQ("""<div class="media-body">""").append(
          jQ(s"""<small class="pull-right time"><i class="fa fa-clock-o"></i> $timeStampStr</small>"""),
          jQ("""<h5 class="media-heading">""").text(message.user.nick),
          jQ("""<small class="col-lg-10">""").text(message.text)
        )
      )
    )
    // scroll to new message
    jQ(".msg-wrap").scrollTop(jQ(".msg-wrap")(0).scrollHeight)
  }
}

class RoomManager(me: User, room: Room, service: ActorRef) extends Actor {
  var roomService: ActorRef = context.system.deadLetters

  val tab = Main.getDiscussionTabInfoOrCreate(room)
  tab.manager = context.self
  tab.messages += Message(User.System, s"Joining room ${room.name} ...")
  tab.focusTab()

  service ! Join(room)

  def receive = LoggingReceive {
    case JoinedRoom(users) =>
      roomService = sender
      context.watch(roomService)
      tab.messages += Message(User.System, s"Joined room ${room.name}")
      tab.users ++= users
      println("JoinedRoom")
      tab.invalidate()

    case UserJoined(user) =>
      tab.users += user
      tab.messages += Message(User.System, s"${user.nick} joined the room")
      tab.invalidate()

    case UserLeft(user) =>
      tab.users -= user
      tab.messages += Message(User.System,
          s"${if (user == me) "You" else user.nick} left the room")
      tab.invalidate()

    case ReceiveMessage(message) =>
      tab.messages += message
      tab.invalidate()

    case Send(text) =>
      roomService ! SendMessage(Message(me, text))

    case Leave =>
      roomService ! Leave
      context.stop(self)

    case Terminated(ref) if ref == roomService =>
      tab.messages += Message(User.System, "The room was deleted")
      tab.invalidate()
  }
}

class ProxyManager extends Actor {
  def receive = {
    case ConnectAs(user) =>
      context.watch(context.actorOf(
          Props(new ClientProxy("ws://localhost:9000/chat-ws-entry", context.parent))))

    case Disconnect =>
      context.children.foreach(context.stop(_))

    case Terminated(proxy) =>
      context.parent ! Disconnected
  }
}
