package nx.comm

import java.util.UUID
import javafx.scene.image.Image

import nx.Util

case class SendableMetadata(guid: UUID, piece: Int, totalPieces: Int, data: Array[Byte]) extends Util
{
	override def toString: String = guid + sep + piece + sep + totalPieces + sep + (data: String)
	def toBytes: Array[Byte] = toString: Array[Byte]
}
object Sendable extends Util
{
	val STRING	= "00000000"
	val IMAGE	= "00000001"
	val FILE	= "00000002"
}

trait Sendable[T <: Any] extends Util
{
	var guid: UUID = null
	def createGUID =
	{
		val temp = UUID.randomUUID.toString
		guid = (obj match
		{
			case str: String => Sendable.STRING
			case im: Image => Sendable.IMAGE
			case ar: Array[Byte] => Sendable.FILE
		}) + temp.splitAt(temp.indexOf('-'))._2: UUID
	}
	val obj: T
	def toBytes: Array[Byte]
	def getPackets(_num: Int): Array[SendableMetadata] =
	{
		val toReturn = new Array[SendableMetadata](_num)
		val byteGroups = toBytes.grouped(_num).toArray
		for (i <- 0 until _num)
			toReturn(i) = SendableMetadata(guid, i, _num, byteGroups(i))
		toReturn
	}
}

class SendableObject[T <: Any](_obj: T, _toBytes: T => Array[Byte] = null) extends Sendable[T]
{
	def this(_obj: T, _guid: String) =
	{
		this(_obj)
		guid = _guid: UUID
	}

	val obj = _obj
	def toBytes = _toBytes(obj)
	createGUID
}

class SendableString(_str: String) extends SendableObject[String](_str)
{
	override def toBytes = obj: Array[Byte]
}

class SendableImage(_img: Image) extends SendableObject[Image](_img)
{
	override def toBytes = obj: Array[Byte]
}

class SendableFile(_fileData: Array[Byte]) extends SendableObject[Array[Byte]](_fileData)
{
	override def toBytes = obj
	def writeFile(_filePath: String) = toFile(_filePath, obj)
}