package nx.comm.sendable

import java.util.UUID

import scala.collection.mutable.ArrayBuffer

class PartialSendable(_guid: UUID, _num: Int)
{
	var guid = _guid
	val parts = new Array[SendableMetadata](_num)

	def addPart(_smd: SendableMetadata) =
		if (_smd.guid.equals(guid))
			parts(_smd.piece - 1) = _smd

	def isComplete: Boolean =
	{
		parts.foreach(p => if (p == null) return false)
		true
	}

	def assemble: Sendable[_] =
	{
		if (!isComplete)
			null
		else
		{
			val data = new ArrayBuffer[Byte]
			parts.foreach(p => data ++= p.data)
			Sendable.construct(parts(0).guid, data.toArray)
		}
	}
}
