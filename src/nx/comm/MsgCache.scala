package nx.comm

import java.util.UUID

import nx.comm.sendable.{SendableMetadata, Sendable, PartialSendable}
import nx.util.Tools

import scala.collection.mutable._

class MsgCache extends Tools
{
	val msgBuffer = new ArrayBuffer[Sendable[_]]
	def getMsg: Sendable[_] =
		if (msgBuffer.length == 0)
			null
		else
		{
			val item = msgBuffer(0)
			sync(msgBuffer.remove(0))
			item
		}

	val partials = new HashMap[UUID, PartialSendable]
	def addPart(_str: String) =
	{
		val smd = SendableMetadata.fromString(_str)
		val partial =
			if (partials(smd.guid) == null)
			{
				val temp = new PartialSendable(smd.guid, smd.totalPieces)
				partials.put(smd.guid, temp)
				temp
			}
			else
				partials(smd.guid)

		partial.addPart(smd)
		if (partial.isComplete)
		{
			msgBuffer += partial.assemble
			partials.remove(partial.guid)
		}
	}
}
