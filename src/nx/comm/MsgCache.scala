package nx.comm

import java.util.UUID

import nx.comm.sendable.{SendableMetadata, Sendable, PartialSendable}
import nx.util.Tools

import scala.collection.mutable._

class MsgCache extends Tools
{
	val msgBuffer = new ArrayBuffer[Sendable[_]]
	def hasMsg = msgBuffer.length > 0
	def getMsg: Sendable[_] = if (hasMsg) msgBuffer.remove(0).^* else null

	val partials = new HashMap[UUID, PartialSendable]
	def addPart(_str: String) =
	{
		val smd = SendableMetadata(_str)
		val partial =
			if (partials.get(smd.guid).isEmpty)
			{
				val temp = new PartialSendable(smd.guid, smd.totalPieces)
				partials.put(smd.guid, temp)
				temp
			}
			else
				partials(smd.guid)

		if (partial.addPart(smd))
		{
			(msgBuffer += partial.assemble).^*
			partials.remove(partial.guid)
		}
	}
}
