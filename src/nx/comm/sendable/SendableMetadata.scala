package nx.comm.sendable

import java.util.UUID

import Sendable._
import nx.util.Tools

object SendableMetadata
{
	def fromString(_str: String) =
	{
		val parts = _str.split(sep)
		SendableMetadata(parts(0): UUID, parts(1).toInt, parts(2).toInt, parts(3): Array[Byte])
	}
}

case class SendableMetadata(guid: UUID, piece: Int, totalPieces: Int, data: Array[Byte]) extends Tools
{
	override def toString: String = guid + sep + piece + sep + totalPieces + sep + (data: String)
	def toBytes: Array[Byte] = toString: Array[Byte]
}
