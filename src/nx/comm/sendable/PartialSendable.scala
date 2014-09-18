package nx.comm.sendable

import java.util.UUID

import nx.util.Tools

import scala.collection.mutable.ArrayBuffer

class PartialSendable(_guid: UUID, _num: Int) extends Tools
{
	var guid = _guid
	val parts = new Array[SendableMetadata](_num)

	def addPart(_smd: SendableMetadata) =
	{
		if (_smd.is(guid))
			parts(_smd.piece - 1) = _smd
		isComplete
	}

	def isComplete: Boolean = parts.filter(_ == null).length == 0
	def assemble: Sendable[_] = if (isComplete) Sendable.construct(parts) else null

	override def toString = s"PartialSendable[${regex("(?:[^\\.]*)$", Sendable.typeFromGUID(guid).tpe)}]\nGUID: ${guid}\n${parts.zipWithIndex.map{case(p, i) => s"Part ${i + 1}: ${if (p == null) "Empty" else "Filled"}"}.mkString("\r\n")}"
}