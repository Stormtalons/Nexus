package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.{ServerSocketChannel, SelectionKey, Selector, SocketChannel}

import nx.{Util, Asynch}

import scala.collection.mutable.ArrayBuffer

class SocketHive extends Asynch with Util
{
	var abandonHive = false
	var hiveQueen = Selector.open
	var populationCap = 1
	val swarm = ArrayBuffer[SocketChannel]()
	def primarySwarmling = if (swarm.length > 0) swarm(0) else null
	def spawn(_host: String, _port: Int) =
	{
		migrate
		SocketChannel.open
			.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
			.configureBlocking(false)
			.register(hiveQueen, SelectionKey.OP_CONNECT)
			.asInstanceOf[SocketChannel]
			.connect(new InetSocketAddress(_host, _port))
	}
	def hatch(_swarmling: SocketChannel): Unit =
	{
		if (swarm.length >= populationCap)
			tryDo(_swarmling.close)
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
				synchronized{swarm += _swarmling}
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
		synchronized{swarm -= _swarmling}
	}

	val spawningPool = ServerSocketChannel.open
	spawningPool.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
	spawningPool.configureBlocking(false)
	def assimilate(_spawnlingCount: Int) =
	{
		populationCap = _spawnlingCount + 1
		spawningPool.register(hiveQueen, SelectionKey.OP_ACCEPT)
		command(s"spawn|${_spawnlingCount}": SendableString)
	}

	def command(_cmd: Sendable[_]) =
	{
		val byteGroups = _cmd.bytes.grouped(swarm.length)
		var i = 0
		while (byteGroups.hasNext)
		{
			tryDo(swarm(i).write(ByteBuffer.wrap(i.toByte +: byteGroups.next union (eom: Array[Byte]))), _e => depart)
			i += 1
		}
	}

	addActivity(
		while (hiveQueen.select > 0)
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

					case swarmling if swarmling.isWritable =>
						val swarmlingMemory = swarmling.attachment.asInstanceOf[ByteBuffer]
						swarmlingMemory.flip
						while (swarmlingMemory.hasRemaining)
							swarmling.channel.asInstanceOf[SocketChannel].write(swarmlingMemory)
				}
		})

	def depart = synchronized
	{
		hiveQueen.close
		swarm.clear
		abandonHive = true
	}
	def settle = synchronized
	{
		abandonHive = false
		hiveQueen = Selector.open
		populationCap = 1
	}
	def migrate =
	{
		depart
		settle
	}

	run
}
