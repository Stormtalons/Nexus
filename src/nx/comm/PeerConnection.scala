package nx.comm

import java.net.{InetSocketAddress, Socket}

class PeerConnection
{
	import nx.Main._

	def this(_host: String, _port: Int) =
	{
		this
		run(connect(_host, _port))
	}
	def this(_socket: Socket) =
	{
		this
		connect(_socket)
	}

	var socket_ = new Socket
	def socket = socket_
	def socket_=(_socket: Socket) =
	{
		recycleable = false
		inBuffer.clear
		outBuffer.clear
		close
		socket_ = _socket
	}

	var recycleable = false

	private val inBuffer = new MsgBuffer
	private val outBuffer = new MsgBuffer
	def hasMsg: Boolean = inBuffer.hasMsg
	def getMsg: String = if (hasMsg) inBuffer.getMsg else null
	def hasObj[T <: Sendable[T]](_key: String): Boolean = inBuffer.hasObj[T](_key)
	def getObj[T <: Sendable[T]](_key: String): T = inBuffer.getObj[T](_key)

	def connect(_socket: Socket) = socket = _socket
	def connect(_host: String, _port: Int) =
		try
		{
			close
			socket.connect(new InetSocketAddress(_host, _port), 5000)
			sendMsg("gimme")
			recycleable = false
		}
		catch
		{
			case e: Exception =>
				log(e.getMessage)
				dispose
		}

	def processTraffic: Unit =
	{
		while (socket.getInputStream.available > 0)
		{
			val data = new Array[Byte](bufferSize)
			var read = 0
			if (!tryy({read = socket.getInputStream.read(data)}, _e => {log(_e.getMessage);dispose}))
				return
			inBuffer << data.dropRight(data.length - read)
		}
		inBuffer.parseMsgs
		while (outBuffer.hasObj)
		{

		}
		while (outBuffer.hasMsg)
		{
			val data: Array[Byte] = outBuffer.getMsg
			var at = 0
			while (at < data.length)
			{
				if (!tryy(socket.getOutputStream.write(data, at, math.min(bufferSize, data.length - at)), _e => {log(_e.getMessage);dispose}))
					return
				at += bufferSize
			}
		}
	}

	def sendMsg(_str: String) =
	{
		outBuffer << _str << eom
		outBuffer.parseMsgs
	}

	def dispose =
	{
		recycleable = true
		inBuffer.clear
		outBuffer.clear
		close
	}
	
	def close = if (socket != null) tryy(socket.close)
}