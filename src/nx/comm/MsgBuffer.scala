package nx.comm

import scala.collection.mutable.ArrayBuffer

class MsgBuffer
{
	private val stage = new ArrayBuffer[Byte]
	private val msgs = new ArrayBuffer[String]

	def add(_bytes: Array[Byte]): Unit = synchronized{stage ++= _bytes}
	def add(_bytes: Byte*): Unit = synchronized{stage ++= _bytes}
	def add(_str: String): Unit = add(_str.getBytes("UTF-8"))

	def parseCompleteMsgs = synchronized
	{
		var i = 0
		while (i < stage.length - 2)
		{
			if (stage(i) == '|' && stage(i + 1) == 'E' && stage(i + 2) == 'M')
			{
				msgs += new String(stage.splitAt(i)._1.toArray, "UTF-8")
				stage.remove(0, i + 3)
				i = -1
			}
			i += 1
		}
	}

	def hasMsg: Boolean = msgs.length > 0
	def getMsg: String = synchronized{if (hasMsg) msgs.remove(0) else null}
}