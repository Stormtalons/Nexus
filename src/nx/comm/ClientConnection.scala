package nx.comm

import java.net.{InetSocketAddress, Socket}

import nx.Util

import scala.collection.mutable.ArrayBuffer

class ClientConnection extends Util
{
	def this(_host: String, _port: Int) =
	{
		this
		ex(connect(_host, _port))
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
	def hasObj[T <: SendableOld[T]](_key: String): Boolean = inBuffer.hasObj[T](_key)
	def getObj[T <: SendableOld[T]](_key: String): T = inBuffer.getObj[T](_key)

	def connect(_socket: Socket) = socket = _socket
	def connect(_host: String, _port: Int): Boolean =
		tryGet[Boolean](
		{
			close
			log(s"Attempting to connect to ${_host}:${_port}")
			socket.connect(new InetSocketAddress(_host, _port), 5000)
			log("Connected successfully")
			sendMsg("gimme")
			recycleable = false
			true
		},
		{
			log("Failed to connect")
			dispose
			false
		})

	def getIP: String = tryGet[String](socket.getLocalAddress.getHostAddress, "")
	def getHost: String = tryGet[String](socket.getLocalAddress.getHostName, "")

	def processTraffic: Unit =
	{
		while (socket.getInputStream.available > 0)
		{
			val data = new Array[Byte](bufferSize)
			var read = 0
			if (!tryDo({read = socket.getInputStream.read(data)}, _e => {log(_e.getMessage);dispose}))
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
				if (!tryDo(socket.getOutputStream.write(data, at, math.min(bufferSize, data.length - at)), _e => {log(_e.getMessage);dispose}))
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
	
	def close = tryDo(socket.close)
}

class Msgs extends Util
{
	val in = ArrayBuffer[SendableOld[_]]()
//	def in_+=(_bytes: Array[Byte]) = synchronized
//	{
//		_bytes(0) match
//		{
//		case Sendable.STRING => in += new SendableString(_bytes)
//		case Sendable.IMAGE => in += new SendableImage(_bytes)
//		case Sendable.FILE => in += new SendableFile(_bytes)
//		}
//	}
	def nextIn = if (in.length > 0) synchronized{in.remove(0)} else null

	val out = ArrayBuffer[SendableOld[_]]()
//	def out_+=(_bytes: Array[Byte]) = synchronized
//	{
//		_bytes(0) match
//		{
//		case Sendable.STRING => out += new SendableString(_bytes)
//		case Sendable.IMAGE => out += new SendableImage(_bytes)
//		case Sendable.FILE => out += new SendableFile(_bytes)
//		}
//	}
	def nextOut = if (out.length > 0) synchronized{out.remove(0)} else null

	def clear = synchronized
	{
		in.clear
		out.clear
	}
}