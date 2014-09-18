package nx.comm

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.{ServerSocketChannel, SelectionKey, Selector, SocketChannel}
import nx.comm.sendable.{SendableMetadata, Sendable}
import nx.util.{Asynch, Tools}

import scala.collection.mutable.ArrayBuffer

class SocketHive extends Asynch with Tools
{
	var hiveQueen: Selector = null

	val repository = new MsgCache
	def hasMsg = repository.hasMsg
	def getMsg = repository.getMsg

	var populationCap = 1
	val swarm = new ArrayBuffer[SocketChannel]
	def primarySwarmling = if (swarm.length > 0) swarm(0) else null
	def spawn(_host: String, _port: Int) =
	{
		migrate
		val spawnling = SocketChannel.open.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
		spawnling.configureBlocking(false)
		spawnling.register(hiveQueen, SelectionKey.OP_CONNECT)
		spawnling.connect(new InetSocketAddress(_host, _port))
	}
	def spawn(_swarmling: SocketChannel): Unit =
	{
		migrate
		hatch(_swarmling)
	}
	def hatch(_swarmling: SocketChannel): Unit =
	{
		if (swarm.length >= populationCap)
			_swarmling.close
		else
		{
			_swarmling.configureBlocking(false)
			if (!_swarmling.isConnected && !_swarmling.isConnectionPending)
			{
				_swarmling.register(hiveQueen, SelectionKey.OP_CONNECT)
				_swarmling.connect(primarySwarmling.getRemoteAddress)
			}
			else
			{
				_swarmling.finishConnect
				_swarmling.register(hiveQueen, SelectionKey.OP_READ)
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
		if (_swarmling.isRegistered)
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

	def command(_obj: Sendable[_]) =
	{
		val bytes = _obj.toBytes
		bytes.grouped(bytes.length / swarm.length).zipWithIndex.foreach{case(data, i) => swarm(i).write(ByteBuffer.wrap(s"${SendableMetadata(_obj.guid, i + 1, swarm.length, data)}${eom}": Array[Byte])).^^#}
	}

	def depart =
	{
		if (hiveQueen != null)
			hiveQueen.close
		swarm.clear
		stop
	}.^^#
	def infest =
	{
		hiveQueen = Selector.open
		populationCap = 1
		run
	}.^^#
	def migrate =
	{
		depart
		waitFor
		infest
	}
	def isInfested = primarySwarmling != null && isRunning && primarySwarmling.isConnected
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
						val toMemorize = ByteBuffer.allocate(32 * 1024)
						swarmling.channel.asInstanceOf[SocketChannel].read(toMemorize)
						toMemorize.flip
						val indexOfEom = indexOf(toMemorize.array, eom: Array[Byte])
						if (indexOfEom != -1)
						{
							val data = new Array[Byte](indexOfEom)
							toMemorize.get(data)
							repository.addPart(data: String)
						}
				}
		})

//	<Dev tools>
	def printSwarm = swarm.foreach(sl => println(sl)).^*.^^#
//	</Dev tools>
}
