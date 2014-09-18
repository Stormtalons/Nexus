package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.channels.{ServerSocketChannel, SelectionKey, Selector}
import javafx.scene.image.Image

import nx.Main
import nx.comm.sendable.Sendable
import nx.util.{Asynch, Tools}

import scala.collection.mutable.ArrayBuffer

class ConnectionManager(_port: Int) extends Asynch with Tools
{
	def this() = this(Main.serverPort_)

	val callbackRegister = Selector.open
	val serverChannel = createServerChannel
	serverChannel.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
	serverChannel.configureBlocking(false)
//	if (instance == 1)
//	{
		serverChannel.bind(new InetSocketAddress(_port))
		serverChannel.register(callbackRegister, SelectionKey.OP_ACCEPT)
//	}
	val outgoingClient = new SocketHive
	val clients = new ArrayBuffer[SocketHive]
	var clientLimit = 5

	def connect(_host: String, _port: Int) = outgoingClient.spawn(_host, _port)

	def send(_obj: Sendable[_]) =
	{
		while (!outgoingClient.isInfested) Thread.sleep(10)
		outgoingClient.command(_obj)
	}

	def printConnections =
	{
		(clients :+ outgoingClient).foreach(peer =>
		{
			println(s"Peer: ${peer}")
			peer.printSwarm
			println
		})
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
							val newHive = new SocketHive
							newHive.spawn(callback.channel.asInstanceOf[ServerSocketChannel].accept)
							(clients += newHive).^*
						}
						else
							callback.channel.asInstanceOf[ServerSocketChannel].accept.close
					case _ =>
				}
		})

	addActivity({
		(clients :+ outgoingClient).foreach(peer =>
		{
			val msg = peer.getMsg
			if (msg != null)
			{
				msg.obj match
				{
					case str: String => println(msg)
					case img: Image =>
					case _ =>
				}
			}
		})
	})

	addCallback({
		callbackRegister.close.^^*
		log("Listening server stopped")
		if (outgoingClient.isInfested)
		{
			log(s"Disconnecting from: ${outgoingClient.getInfestment}")
			outgoingClient.depart
		}
		if (clients.length > 0)
		{
			log(s"Dropped ${clients.length} active peers")
			clients.foreach(_client => _client.depart)
			clients.clear
		}
	})

	run
}