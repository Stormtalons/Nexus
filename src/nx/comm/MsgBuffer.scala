package nx.comm

import nx.Main

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

class MsgBuffer
{
	import Main._

	private val stage = new ArrayBuffer[Byte]
	private val msgs = new ArrayBuffer[String]

	def <<(_bytes: Array[Byte]): MsgBuffer = synchronized{stage ++= _bytes;this}
	def <<(_bytes: Byte*): MsgBuffer = synchronized{stage ++= _bytes;this}
	def <<(_chars: Array[Char]): MsgBuffer = <<(_chars.map(_.toByte))
	def <<(_str: String): MsgBuffer = <<(_str: Array[Byte])

	def parseCompleteMsgs = synchronized
	{
		var i = 0
		while (i <= stage.length - eom.length)
		{
			breakable
			{
				for (j <- 0 to eom.length - 1)
					if (stage(i + j) != eom(j))
						break
				msgs += stage.splitAt(i + eom.length)._1.toArray[Byte]
				stage.remove(0, i + eom.length)
				i = -1
			}
			i += 1
		}
	}

	def hasMsg: Boolean = synchronized{msgs.length > 0}
	def getMsgStr: String = synchronized{if (hasMsg) msgs.remove(0) else null}
	def getMsgBytes: Array[Byte] = getMsgStr

	def clear = synchronized
	{
		stage.clear
		msgs.clear
	}
}