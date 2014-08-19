package nx.comm

import java.net.{SocketTimeoutException, InetSocketAddress, Socket}

import nx.Main

class PeerConnection
{
	import Main._

	def this(_host: String, _port: Int) =
	{
		this
		run(connect(_host, _port))
	}
	def this(_socket: Socket) =
	{
		this
		receiveConnection(_socket)
	}

	var socket: Socket = null
	var recycleable = false

	private val inBuffer = new MsgBuffer
	private val outBuffer = new MsgBuffer
	def hasMsg: Boolean = inBuffer.hasMsg
	def getMsgStr: String = if (hasMsg) inBuffer.getMsgStr else null
	def getMsgBytes: Array[Byte] = getMsgStr

	def connect(_host: String, _port: Int) =
	{
		socket = new Socket
		try
		{
			socket.connect(new InetSocketAddress(_host, _port), 5000)
			send("gimme")
		} catch {case e: SocketTimeoutException => log(e.getMessage);dispose}
	}

	def receiveConnection(_socket: Socket) =
	{
		socket = _socket
	}

	def processTraffic =
	{
		try
		{
			while (socket.getInputStream.available > 0)
			{
				val data = new Array[Byte](bufferSize)
				val read = socket.getInputStream.read(data)
				inBuffer << data.dropRight(data.length - read)
			}
			inBuffer.parseCompleteMsgs
			while(outBuffer.hasMsg)
			{
				val data = outBuffer.getMsgBytes
				var at = 0
				while (at < data.length)
				{
					socket.getOutputStream.write(data, at, math.min(bufferSize, data.length - at))
					at += bufferSize
				}
				socket.getOutputStream.flush
			}
		} catch {case e: Exception => log(e.getMessage);e.printStackTrace();dispose}
	}

	def send(_str: String) =
	{
		outBuffer << _str << eom
		outBuffer.parseCompleteMsgs
	}

	def dispose =
	{
		recycleable = true
		inBuffer.clear
		outBuffer.clear
		try socket.close catch{case e: Exception => log(e.getMessage)}
	}
}