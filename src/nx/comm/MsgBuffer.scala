package nx.comm

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

class MsgBuffer
{import nx.Main._
	private val stage = new ArrayBuffer[Byte]
	def <<(_bytes: Array[Byte]): MsgBuffer = synchronized{stage ++= _bytes;this}
	def <<(_bytes: Byte*): MsgBuffer = synchronized{stage ++= _bytes;this}
	def <<(_chars: Array[Char]): MsgBuffer = <<(_chars.map(_.toByte))
	def <<(_str: String): MsgBuffer = <<(_str: Array[Byte])

	private val msgs = new ArrayBuffer[String]
	def hasMsg: Boolean = msgs.length > 0
	def getMsg: String = synchronized{if (hasMsg) msgs.remove(0) else null}

	private var objs = HashMap[String, AnyRef]()
	def addObj[T <: Sendable[T]](_obj: T) = synchronized{objs += ((_obj.label, _obj))}
	def hasObj: Boolean = objs.size > 0
	def hasObj[T <: Sendable[T]](_key: String): Boolean = synchronized
	{
		try
		{
			val obj = objs.get(_key).asInstanceOf[T]
			obj == null || !obj.isInstanceOf[T]
		}
		catch {case e: Exception => false}
	}
	def getObj: AnyRef = getObj(objs.keysIterator.next)
	def getObj[T <: Sendable[T]](_key: String): T = synchronized{objs.get(_key).asInstanceOf[T]}

	def clear = synchronized
	{
		stage.clear
		msgs.clear
		objs = HashMap[String, AnyRef]()
	}

	def parseMsgs = synchronized
	{
		var i = 0
		while (i <= stage.length - eom.length)
		{
			breakable
			{
				for (j <- 0 to eom.length - 1)
					if (stage(i + j) != eom(j))
						break
				var msgBytes = stage.splitAt(i + eom.length)._1.toArray[Byte]
				stage.remove(0, i + eom.length)
				val msgType = msgBytes(0)
				msgBytes = msgBytes.drop(1)
				msgType match {
					case Sendable.STRING => msgs += new SendableString(msgBytes).obj
					case Sendable.IMAGE => addObj[SendableImage](new SendableImage(msgBytes))
					case Sendable.FILE => addObj[SendableFile](new SendableFile(msgBytes))
				}
				i = -1
			}
			i += 1
		}
	}
}