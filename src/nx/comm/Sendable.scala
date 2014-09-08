package nx.comm

import java.util.UUID
import javafx.scene.image.Image

import nx.Util

import scala.collection.immutable.HashMap
import scala.reflect.runtime.universe._

case class SendableMetadata(guid: UUID, piece: Int, totalPieces: Int, data: Array[Byte]) extends Util
{
	override def toString: String = guid + sep + piece + sep + totalPieces + sep + (data: String)
	def toBytes: Array[Byte] = toString: Array[Byte]
}
object Sendable extends Util
{import scala.language.existentials

	val STRING	= "00000001"
	val IMAGE	= "00000002"
	val FILE	= "00000003"
//[String, (TypeTag[_], TypeTag[_], _ => Array[Byte], Array[Byte] => _)]
	private val typeDict = HashMap(
		STRING -> (typeTag[String],			typeTag[Sendable[String]],		(_str: String) => _str: Array[Byte],	(_data: Array[Byte]) => _data: String),
		IMAGE  -> (typeTag[Image],			typeTag[Sendable[Image]],		(_img: Image) => _img: Array[Byte],		(_data: Array[Byte]) => _data: Image),
		FILE   -> (typeTag[Array[Byte]],	typeTag[Sendable[Array[Byte]]],	(_fileData: Array[Byte]) => _fileData,	(_data: Array[Byte]) => _data)
	)

	def getDataType(_type: String) = typeDict(_type)._1.tpe
	def getWrapperType(_type: String) = typeDict(_type)._2.tpe
	def getMirror(_type: String) = typeDict(_type)._2.mirror
	def getConvertToBytes[T](_type: String) = typeDict(_type)._3.asInstanceOf[T => Array[Byte]]
	def getConvertToObj[T](_type: String) = typeDict(_type)._4.asInstanceOf[Array[Byte] => T]

//Returns each of the above tools in a set for efficiency if all are needed
//	._1 -> Data Type
//	._2 -> Wrapper Type
//	._3 -> Mirror
//	._4 -> Function literal for deconstructing the object into bytes
//	._5 -> Function literal for reconstructing the object from bytes
	def getReflectionTools(_type: String) = {val typeMeta = typeDict(_type);(typeMeta._1.tpe, typeMeta._2.tpe, typeMeta._2.mirror, typeMeta._3, typeMeta._4)}

	def construct(_type: String, _data: Array[Byte]): Sendable[_] =
	{
		val (dataType, objType, mirror, _, bytesToObj) = getReflectionTools(_type)
		val obj = mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asMethod)(bytesToObj(_data))

		dataType match
		{
			case dt if dt =:= typeOf[String] => obj.asInstanceOf[Sendable[String]]
			case dt if dt =:= typeOf[Image] => obj.asInstanceOf[Sendable[Image]]
			case dt if dt =:= typeOf[Array[Byte]] => obj.asInstanceOf[Sendable[Array[Byte]]]
			case _ => obj.asInstanceOf[Sendable[_]]
		}
	}
}

class Sendable[T: TypeTag](_obj: T, _guid: UUID = null) extends Util
{import nx.comm.Sendable._

	val objType = typeOf[T] match
	{
		case t if t =:= typeOf[String] => STRING
		case t if t =:= typeOf[Image] => IMAGE
		case t if t =:= typeOf[Array[Byte]] => FILE
		case _ => "00000000"
	}

	val guid = if (_guid != null) _guid else objType + "-" + UUID.randomUUID.toString.split("-", 2)(1): UUID

	val obj = _obj

	def toBytes = getConvertToBytes(objType)(obj)

	def getPackets(_num: Int): Array[SendableMetadata] =
	{
		val toReturn = new Array[SendableMetadata](_num)
		val byteGroups = toBytes.grouped(_num).toArray
		for (i <- 0 until _num)
			toReturn(i) = SendableMetadata(guid, i, _num, byteGroups(i))
		toReturn
	}
}