package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.channels.{ServerSocketChannel, SelectionKey, Selector}

import nx.util.{Asynch, Tools}

import scala.collection.mutable.ArrayBuffer

class ConnectionManager extends Asynch with Tools
{
	val callbackRegister = Selector.open
	val serverChannel = createServerChannel
	serverChannel.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
	serverChannel.configureBlocking(false)
	if (instance == 1)
	{
		serverChannel.bind(new InetSocketAddress("0.0.0.0", serverPort))
		serverChannel.register(callbackRegister, SelectionKey.OP_ACCEPT)
	}
	val outgoingClient = new SocketHive
	val clients = new ArrayBuffer[SocketHive]
	var clientLimit = 5

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
		outgoingClient.getMsg
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

	def connect(_host: String, _port: Int) = outgoingClient.spawn(_host, _port)

	run
}