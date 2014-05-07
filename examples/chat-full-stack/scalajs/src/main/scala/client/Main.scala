package client

import scala.language.postfixOps

import scala.collection.mutable
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.Ask.ask
import akka.scalajs.wsclient._
import akka.event.LoggingReceive
import akka.util.Timeout
import akka.scalajs.jsapi.Timers

import models._

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.jquery.{jQuery => jQ, _}

@JSExport("Client")
object Main {
  RegisterPicklers.registerPicklers()

  val notifications = jQ("#notifications")

  val system = ActorSystem("chat-client")
  val manager = system.actorOf(Props(new Manager), name = "manager")
  var me: User = User.Nobody

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
  val privateChatTabInfos = mutable.Map.empty[User, DiscussionTabInfo]

  def getDiscussionTabInfoOrCreate(room: Room): DiscussionTabInfo = {
    roomTabInfos.getOrElseUpdate(room, new DiscussionTabInfo("#"+room.name))
  }

  def getPrivateChatTabInfoOrCreate(dest: User): DiscussionTabInfo = {
    privateChatTabInfos.getOrElseUpdate(dest, new DiscussionTabInfo("@"+dest.nick))
  }

  @JSExport
  def startup(): Unit = {
    jQ(".not-landing").hide()

    jQ("#connect-button") click {
      (event: JQueryEventObject) =>
        connect()
        false
    }

    roomsTab.focusTab()
  }

  def connect(): Unit = {
    val nickname = jQ("#nickname-edit").value().toString()
    val email = jQ("#email-edit").value().toString()
    if (nickname == "") {
      dom.alert("Nickname cannot be empty")
      return
    }

    val gravatarHash = computeGravatarHash(email)
    me = User(nickname, gravatarHash)
    manager ! AttemptToConnect

    jQ(".landing").fadeOut("fast")
    Timers.setInterval(200) {
      jQ(".not-landing").fadeIn("fast")
    }
  }

  def joinRoom(room: Room): Unit = {
    roomTabInfos.get(room).fold {
      manager ! Join(room)
    } {
      _.focusTab()
    }
  }

  def startPrivateChat(dest: User): Unit = {
    privateChatTabInfos.get(dest).fold {
      manager ! CreatePrivateChatRoom(dest)
    } {
      _.focusTab()
    }
  }

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
  val tabButton = jQ("""<a href="#">""").text(name+" ").appendTo(tabLi)
  tabButton click { (e: JQueryEventObject) => focusTab(); false }
  jQ("""<span class="badge">""").appendTo(tabButton)

  lazy val tabBadge = tabButton.children().filter("span")

  def badgeText: String = tabBadge.text()
  def badgeText_=(v: String): Unit = tabBadge.text(v)

  def isFocused: Boolean = Main.focusedTab eq this

  def focusTab(): Unit = {
    Main.focusedTab = this
  }

  def activate(): Unit = {
    assert(isFocused, s"Trying to activate non-focused tab $name")
    tabLi.addClass("active")
    invalidate()
  }

  def deactivate(): Unit = {
    tabLi.removeClass("active")
    invalidate()
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
  tabButton.html(tabButton.html() + "&nbsp;")
  val closeButton = jQ("""<button type="button" class="close" aria-hidden="true">&times;</button>""").appendTo(tabButton)
  closeButton click { (e: JQueryEventObject) => closeTab(); false }

  def closeTab(): Unit = {
    Main.roomsTab.focusTab()
    Main.roomTabInfos.find(_._2 eq this).foreach(Main.roomTabInfos -= _._1)
    Main.privateChatTabInfos.find(_._2 eq this).foreach(Main.privateChatTabInfos -= _._1)
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

class DiscussionTabInfo(nme: String) extends TabInfo(nme) with CloseableTab {

  private[this] var myManager: ActorRef = Main.system.deadLetters
  def manager: ActorRef = myManager
  def manager_=(v: ActorRef): Unit = {
    myManager = v
    if (isFocused)
      SendMessageButton.target = v
  }

  val users = new mutable.ListBuffer[User]
  val messages = new mutable.ListBuffer[Message]
  var messagesSeen: Int = 0

  override def invalidate(): Unit = {
    super.invalidate()

    if (isFocused)
      messagesSeen = messages.size

    val newMessages = messages.drop(messagesSeen).count(_.user != User.System)
    badgeText = if (newMessages == 0) "" else newMessages.toString
  }

  override def render(): Unit = {
    super.render()
    renderUsers()
    renderMessages()
  }

  def renderUsers(): Unit = {
    import Main._
    for (user <- users) {
      UsersContainer.addEntry(user.nick, gravatarURL(user, 50)) {
        if (user != Main.me) {
          jQ("""<a href="#" role="button"><span class="glyphicon glyphicon-user"></span> Private chat</a>""").click {
            (e: JQueryEventObject) => startPrivateChat(user); false
          }
        } else jQ()
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

case object AttemptToConnect
case class Send(text: String)
case class CreatePrivateChatRoom(dest: User)
case class AcceptPrivateChatWith(peerUser: User, peer: ActorRef)
case object Disconnect
case object Disconnected

class Manager extends Actor {
  val proxyManager = context.actorOf(Props(new ProxyManager))

  private[this] var myStatusAlert: JQuery = jQ()
  def statusAlert: JQuery = myStatusAlert
  def statusAlert_=(v: JQuery): Unit = {
    myStatusAlert.remove()
    myStatusAlert = v
    myStatusAlert.prependTo(Main.notifications)
  }

  def makeStatusAlert(style: String): JQuery =
    jQ(s"""<div class="alert alert-$style">""")

  def receive = disconnected()

  def disconnected(nextReconnectTimeout: FiniteDuration = 1 seconds): Receive = LoggingReceive {
    case m @ AttemptToConnect =>
      proxyManager ! m
      statusAlert = makeStatusAlert("info").append(
        jQ("<strong>Connecting ...</strong>"),
        jQ("<span> </span>").append(
          jQ("""<a href="#">""").text("Cancel").click {
            (e: JQueryEventObject) =>
              self ! Disconnect
              false
          }
        )
      )
      context.setReceiveTimeout(5 seconds)
      context.become(connecting(nextReconnectTimeout))
  }

  def waitingToAutoReconnect(autoReconnectDeadline: FiniteDuration,
      nextReconnectTimeout: FiniteDuration): Receive = LoggingReceive {
    case m @ AttemptToConnect =>
      context.become(disconnected(nextReconnectTimeout))
      self ! m

    case m @ ReceiveTimeout =>
      val now = nowDuration
      if (now >= autoReconnectDeadline)
        self ! AttemptToConnect
      else {
        val remaining = autoReconnectDeadline - nowDuration
        jQ(".reconnect-remaining-seconds").text(remaining.toSeconds.toString)
      }
  }

  def connecting(nextReconnectTimeout: FiniteDuration): Receive = LoggingReceive { withDisconnected(nextReconnectTimeout) {
    case ReceiveTimeout | Disconnect =>
      context.setReceiveTimeout(Duration.Undefined)
      proxyManager ! Disconnect

    case m @ WebSocketConnected(entryPoint) =>
      context.setReceiveTimeout(Duration.Undefined)

      val service = entryPoint
      service ! Connect(Main.me)
      val alert = makeStatusAlert("success").append(
        jQ("<strong>").text(s"Connected as ${Main.me.nick}")
      )
      statusAlert = alert
      Timers.setTimeout(3000) {
        alert.fadeOut()
      }

      for ((room, tab) <- Main.roomTabInfos)
        self ! Join(room)
      for ((peerUser, tab) <- Main.privateChatTabInfos)
        self ! CreatePrivateChatRoom(peerUser)

      context.become(connected(service))
  } }

  def connected(service: ActorRef): Receive = LoggingReceive { withDisconnected() {
    case RoomListChanged(rooms) =>
      Main.roomsTab.rooms = rooms

    case m @ Disconnect =>
      proxyManager ! m

    case m @ Join(room) =>
      context.actorOf(Props(new RoomManager(room, service)))

    case m @ CreatePrivateChatRoom(dest) =>
      context.actorOf(Props(new PrivateChatManager(dest, service)))

    case m @ RequestPrivateChat(peerUser) =>
      val peer = sender
      Main.privateChatTabInfos.get(peerUser).fold[Unit] {
        val notification: JQuery =
          jQ("""<div class="alert alert-info alert-block">""")
        notification.append(
          jQ("""<h4>""").text(s"Chat with ${peerUser.nick}?"),
          jQ("""<span>""").text(
              s"${peerUser.nick} would like to start a private with you."),
          jQ("""<br>"""),
          jQ("""<button class="btn btn-success">Accept</button>""").click {
            (e: JQueryEventObject) =>
              notification.remove()
              self ! AcceptPrivateChatWith(peerUser, peer)
          },
          jQ("""<button class="btn btn-danger">Reject</button>""").click {
            (e: JQueryEventObject) =>
              notification.remove()
              peer.tell(RejectPrivateChat, Actor.noSender) // do not tell him who I am!
          }
        )
        Main.notifications.append(notification)
      } { tab =>
        // auto-reaccept if we already have a tab for that user
        context.actorOf(Props(new PrivateChatManager(peerUser, service, peer)))
      }

    case m @ AcceptPrivateChatWith(peerUser, peer) =>
      context.actorOf(Props(new PrivateChatManager(peerUser, service, peer)))
  } }

  def withDisconnected(nextReconnectTimeout: FiniteDuration = 1 seconds)(
      receive: Receive): Receive = receive.orElse[Any, Unit] {
    case Disconnected =>
      context.children.filterNot(proxyManager == _).foreach(context.stop(_))
      statusAlert = makeStatusAlert("danger").append(
        jQ("""<strong>You have been disconnected from the server.</strong>"""),
        jQ("""<span>Will try to reconnect in """+
            """<span class="reconnect-remaining-seconds"></span>"""+
            """ seconds. </span>""").append(
          jQ("""<a href="#">""").text("Reconnect now").click {
            (e: JQueryEventObject) =>
              self ! AttemptToConnect
          }
        )
      )
      jQ(".reconnect-remaining-seconds").text(nextReconnectTimeout.toSeconds.toString)
      val autoReconnectDeadline = nowDuration + nextReconnectTimeout
      context.setReceiveTimeout(1 seconds)
      context.become(waitingToAutoReconnect(
          autoReconnectDeadline, nextReconnectTimeout*2))
  }

  private def nowDuration = System.currentTimeMillis() milliseconds
}

class RoomManager(room: Room, service: ActorRef) extends Actor {
  import Main.me

  var roomService: ActorRef = context.system.deadLetters

  val tab = Main.getDiscussionTabInfoOrCreate(room)
  tab.manager = context.self
  tab.messages += Message(User.System, s"Joining room ${room.name} ...")
  tab.focusTab()

  service ! Join(room)

  override def postStop(): Unit = {
    super.postStop()
    tab.users.clear()
    tab.invalidate()
  }

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
      context.stop(self)
  }
}

class PrivateChatManager private (
    dest: User, var peer: ActorRef, _dummy: Int) extends Actor {
  import Main.me

  if (peer eq null)
    peer = context.system.deadLetters

  val tab = Main.getPrivateChatTabInfoOrCreate(dest)
  tab.manager = context.self
  if (tab.users.isEmpty)
    tab.users ++= Seq(me, dest)

  def this(dest: User, service: ActorRef) = {
    this(dest, null, 0)
    tab.messages += Message(User.System,
        s"Asking ${dest.nick} to start a private chat ...")
    service ! RequestPrivateChat(dest)
    tab.focusTab()
  }

  def this(dest: User, service: ActorRef, peer: ActorRef) = {
    this(dest, peer, 0)
    context.watch(peer)
    tab.messages += Message(User.System,
        s"You have accepted ${dest.nick}'s invitation to chat privately.")
    peer ! AcceptPrivateChat
    tab.focusTab()
  }

  def receive = LoggingReceive {
    case AcceptPrivateChat =>
      peer = sender
      context.watch(peer)
      tab.messages += Message(User.System,
          s"${dest.nick} accepted the private chat.")
      tab.invalidate()

    case RejectPrivateChat =>
      tab.messages += Message(User.System,
          s"${dest.nick} denied your request to chat privately.")
      tab.invalidate()

    case SendMessage(message) => // not ReceiveMessage: symmetric connection
      tab.messages += message
      tab.invalidate()

    case Send(text) =>
      val msg = Message(me, text)
      tab.messages += msg
      peer ! SendMessage(msg)
      tab.invalidate()

    case Leave =>
      context.stop(self)

    case Terminated(ref) if ref == peer =>
      tab.messages += Message(User.System, s"${dest.nick} left the chat.")
      tab.invalidate()
  }
}

class ProxyManager extends Actor {
  def receive = {
    case AttemptToConnect =>
      context.watch(context.actorOf(
          Props(new ClientProxy("ws://localhost:9000/chat-ws-entry", context.parent))))

    case Disconnect =>
      context.children.foreach(context.stop(_))

    case Terminated(proxy) =>
      context.parent ! Disconnected
  }
}
