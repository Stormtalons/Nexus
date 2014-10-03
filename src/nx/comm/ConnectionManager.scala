package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, ServerSocketChannel, SelectionKey, Selector}
import java.util.UUID
import javafx.scene.image.Image

import akka.actor.{ActorSystem, ActorRef, Props, Actor}
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import akka.routing.RoundRobinRouter
import nx.Main
import nx.comm.sendable.{SendableMetadata, PartialSendable, Sendable}
import nx.util.Tools
import sw.common.Asynch

import scala.collection.mutable.{HashMap, ArrayBuffer}

class ConnectionManager(_port: Int) extends Actor with Tools with Asynch
{
	def this() = this(Main.serverPort_)

	implicit val system = ActorSystem("IO")

	val callbackRegister = Selector.open
	val serverChannel = createServerChannel
	serverChannel.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
	serverChannel.configureBlocking(false)
	serverChannel.bind(new InetSocketAddress(_port))
	serverChannel.register(callbackRegister, SelectionKey.OP_ACCEPT)
	val outgoingClient = context.actorOf(Props(new Client))
	val clients = new ArrayBuffer[ActorRef]
	var clientLimit = 5

	IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", _port))

	def receive =
	{
		case Bound(localAddress) => println("Bound to: " + localAddress)
		case CommandFailed(_: Bind) => context stop self
		case Connected(remote, local) =>
			println("Connected to: " + remote)
			val handler = context.actorOf(Props(new Client))
			val connection = sender()
			connection ! Register(handler)
		case c: Connect => outgoingClient ! c
	}

	addActivity(
		while (callbackRegister.isOpen && callbackRegister.select > 0)
		{
			val availableCallbacks = callbackRegister.selectedKeys.iterator
			while (availableCallbacks.hasNext)
				availableCallbacks.next match
				{
					case callback if callback.isAcceptable =>
						if (clients.length < clientLimit)
						{
							val client = context.actorOf(Props(new Client))
							client ! callback.channel.asInstanceOf[ServerSocketChannel].accept
							(clients += client).^*
						}
						else
							callback.channel.asInstanceOf[ServerSocketChannel].accept.close
					case _ =>
				}
		})
//
//	addActivity({
//		(clients :+ outgoingClient).foreach(peer =>
//		{
//			if (peer.hasItem)
//			{
//				val item = peer.getItem
//				item.obj match
//				{
//					case str: String => println(item)
//					case img: Image =>
//					case _ =>
//				}
//			}
//			if (peer.hasMsg)
//			{
//				val msg = peer.getMsg.split("\\|")
//				if (msg(0).equals("GET"))
//				{
//					if (msg(1).equals("REPLICATION"))
//					{
//
//					}
//					else if (msg(1).equals("ITEM"))
//					{
//
//					}
//
//				}
//			}
//		})
//	})
//
//	addCallback({
//		callbackRegister.close.^^*
//		log("Listening server stopped")
//		if (outgoingClient.isInfested)
//		{
//			log(s"Disconnecting from: ${outgoingClient.getInfestment}")
//			outgoingClient.depart
//		}
//		if (clients.length > 0)
//		{
//			log(s"Dropped ${clients.length} active peers")
//			clients.foreach(_client => _client.depart)
//			clients.clear
//		}
//	})
//
//	run
}