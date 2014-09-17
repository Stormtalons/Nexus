package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.{ServerSocketChannel, SelectionKey, Selector, SocketChannel}
import nx.util.{Asynch, Tools}

import scala.collection.mutable.ArrayBuffer

class SocketHive extends Asynch with Tools
{
	var hiveQueen: Selector = null

	val repository = new MsgCache
	def getMsg = repository.getMsg

	var populationCap = 1
	val swarm = ArrayBuffer[SocketChannel]()
	def primarySwarmling = if (swarm.length > 0) swarm(0) else null
	def spawn(_host: String, _port: Int): Unit =
	{
		migrate
		SocketChannel.open
			.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
			.configureBlocking(false)
			.register(hiveQueen, SelectionKey.OP_CONNECT)
			.asInstanceOf[SocketChannel]
			.connect(new InetSocketAddress(_host, _port))
	}
	def spawn(_swarmling: SocketChannel): Unit =
	{
		migrate
		hatch(_swarmling)
	}
	def hatch(_swarmling: SocketChannel): Unit =
	{
		if (swarm.length >= populationCap)
			_swarmling.close.^#
		else
		{
			_swarmling
				.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
				.configureBlocking(false)
			if (!_swarmling.isConnected)
				_swarmling
					.register(hiveQueen, SelectionKey.OP_CONNECT)
					.asInstanceOf[SocketChannel]
					.connect(primarySwarmling.getRemoteAddress)
			else
			{
				_swarmling.keyFor(hiveQueen).cancel
				_swarmling.register(hiveQueen, SelectionKey.OP_READ | SelectionKey.OP_WRITE)
				(swarm += _swarmling).^*
			}
		}
	}
	def hatch(_spawnlingCount: Int): Unit =
		for (i <- 0 until _spawnlingCount)
			if (swarm.length < populationCap)
				hatch(SocketChannel.open)
	def killSwarmling(_swarmling: SocketChannel) =
	{
		_swarmling.keyFor(hiveQueen).cancel
		_swarmling.close
		(swarm -= _swarmling).^*
	}

	val spawningPool = ServerSocketChannel.open
	spawningPool.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
	spawningPool.configureBlocking(false)
	def assimilate(_spawnlingCount: Int) =
	{
		populationCap = _spawnlingCount + 1
		spawningPool.register(hiveQueen, SelectionKey.OP_ACCEPT)
//		command(s"spawn|${_spawnlingCount}")
	}

//	def command(_obj: Sendable[_]) =
//	{
//		val packets = _obj.getPackets(swarm.length)
//		for (i <- 0 until packets.length)
//			ex(synchronized
//			{
//				tryDo(swarm(i).write(ByteBuffer.wrap(packets(i).toString + eom: Array[Byte])), _e => depart)
//			})
//	}

	def depart =
	{
		hiveQueen.close
		swarm.clear
		stop
	}.^*
	def infest =
	{
		hiveQueen = Selector.open
		populationCap = 1
		run
	}.^*
	def migrate =
	{
		depart
		waitFor
		infest
	}
	def isInfested = isRunning && primarySwarmling.isConnected
	def getInfestment = if (isInfested) primarySwarmling.getRemoteAddress else null

	addActivity(
		while (hiveQueen.isOpen && hiveQueen.select > 0)
		{
			val hive = hiveQueen.selectedKeys.iterator
			while (hive.hasNext)
				hive.next match
				{
				case swarmling if swarmling.isAcceptable => hatch(swarmling.channel.asInstanceOf[ServerSocketChannel].accept)
				case swarmling if swarmling.isConnectable => hatch(swarmling.channel.asInstanceOf[SocketChannel])
				case swarmling if swarmling.isReadable =>
					val swarmlingMemory = swarmling.attachment.asInstanceOf[ByteBuffer]
					val toMemorize = ByteBuffer.allocate(32 * 1024)
					swarmling.channel.asInstanceOf[SocketChannel].read(toMemorize)
					toMemorize.flip
					swarmlingMemory.limit(swarmlingMemory.capacity)
					swarmlingMemory.put(toMemorize)
					val indexOfEom = indexOf(swarmlingMemory.array, eom: Array[Byte])
					if (indexOfEom != -1)
					{
						val data = new Array[Byte](indexOfEom)
						swarmlingMemory.get(data)
						repository.addPart(data: String)
					}
				case swarmling if swarmling.isWritable =>
					val swarmlingMemory = swarmling.attachment.asInstanceOf[ByteBuffer]
					swarmlingMemory.flip
					while (swarmlingMemory.hasRemaining)
						swarmling.channel.asInstanceOf[SocketChannel].write(swarmlingMemory)
				}
		})
}
