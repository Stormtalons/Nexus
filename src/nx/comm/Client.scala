package nx.comm

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor._
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

import nx.Main
import nx.comm.sendable.{SendableMetadata, PartialSendable, Sendable}
import nx.util.Tools

import scala.collection.mutable.{HashMap, ArrayBuffer}

case class O(host: String, port: Int)
class Client(implicit val system: ActorSystem) extends Actor with Tools
{
	val itemCache = new HashMap[UUID, PartialSendable]
	val msgs = new ArrayBuffer[String]

	def receive =
	{
		case Bind =>
			IO(Tcp) ! Bind(self, new InetSocketAddress("127.0.0.1", serverPort))

		case CommandFailed(b: Bind) =>
			log(s"Error binding to ${b.localAddress}")
			serverPort = serverPort + 1
			self ! Bind

		case O(host, port) =>
			IO(Tcp) ! Connect(new InetSocketAddress(host, port))

		case CommandFailed(c: Connect) =>
			log(s"Error connecting to ${c.remoteAddress}")

		case Connected(remote, local) =>
			val connection = sender
			connection ! Register(self)
			context become
			{
				case send: Sendable[_] =>
					send.getPackets(2).foreach(packet =>
					{
						connection ! Write(ByteString(packet.toBytes))
						sleep(10)
					})

				case CommandFailed(w: Write) =>
					log(s"Error writing to ${remote}")
					self ! Close

				case Received(data) =>
					val msg = SendableMetadata(data.toArray)
					println("Received: " + msg)
					val partial =
						if (itemCache.get(msg.guid).nonEmpty)
							itemCache.get(msg.guid).get
						else
						{
							val temp = new PartialSendable(msg.guid, msg.totalPieces)
							itemCache.put(msg.guid, temp)
							temp
						}
					if (partial.addPart(msg))
					{
						Main.receivedItems_.put(partial.guid, partial.assemble)
						itemCache.remove(partial.guid)
					}

				case Close =>
					connection ! Close

				case _: ConnectionClosed =>
					context.unbecome

				case PeerClosed =>
					context.unbecome
			}
	}
}








