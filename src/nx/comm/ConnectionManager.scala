package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions, Socket}
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, ServerSocketChannel, SelectionKey, Selector}

import nx.util.{Asynch, JSON, Tools}
import nx.Main

import scala.collection.mutable.ArrayBuffer

class ConnectionManager extends Asynch with Tools
{
	val callbackRegister = Selector.open
	val serverChannel = createServerChannel
	serverChannel.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
	serverChannel.bind(new InetSocketAddress("0.0.0.0", serverPort))
	serverChannel.register(callbackRegister, SelectionKey.OP_ACCEPT)
	val outgoingClient = new SocketHive
	val clients = new ArrayBuffer[SocketHive]
	var clientLimit = 5

	addActivity(
		while (callbackRegister.select > 0)
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
							sync(clients += newHive)
						}
						else
							callback.channel.asInstanceOf[ServerSocketChannel].accept.close
					case _ =>
				}
		})

	addActivity({
		outgoingClient.getMsg
	})

	override def callback = () =>
	{
		log("Stopping listening server")
		serverChannel.keyFor(callbackRegister).cancel
		serverChannel.close
		log("Listening server stopped")
		log(s"Disposing of ${clients.length} clients")
		outgoingClient.depart
		sync({
			clients.foreach(_client => _client.depart)
			clients.clear
		})
		log("Client list cleared")
	}

	def connect(_host: String, _port: Int) = outgoingClient.spawn(_host, _port)

	run
}