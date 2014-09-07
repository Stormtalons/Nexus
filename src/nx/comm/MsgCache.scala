package nx.comm

import java.util.UUID

import nx.Util

import scala.collection.mutable._

class MsgCache extends Util
{
	class Partial(_length: Int)
	{
		val parts = new Array[SendableMetadata](_length)
	}
	
	val msgBuffer = ArrayBuffer[Sendable[_]]()
	val partials = new HashMap[UUID, Partial]
}
