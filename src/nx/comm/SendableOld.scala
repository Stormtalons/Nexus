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
object SendableOld extends Util
{import scala.language.existentials

	implicit def strToTypeTag(_str: String): TypeTag[_] = typeIndex(_str)

	val NONE	= "00000000"
	val STRING	= "00000001"
	val IMAGE	= "00000002"
	val FILE	= "00000003"

	private val typeIndex = HashMap[String, TypeTag[_]](
		STRING -> typeTag[String],
		IMAGE  -> typeTag[Image],
		FILE   -> typeTag[Array[Byte]]
	)

	private val typeMetadata = HashMap[TypeTag[_], (String, TypeTag[_], _, Array[Byte] => _)](
		typeTag[String]		 -> (STRING,	typeTag[SendableOld[String]],			(_str: String) => _str: Array[Byte],	(_data: Array[Byte]) => _data: String),
		typeTag[Image]		 -> (IMAGE,		typeTag[SendableOld[Image]],			(_img: Image) => _img: Array[Byte],		(_data: Array[Byte]) => _data: Image),
		typeTag[Array[Byte]] -> (FILE,		typeTag[SendableOld[Array[Byte]]],		(_fileData: Array[Byte]) => _fileData,	(_data: Array[Byte]) => _data)
	)
	
	def typeID[T: TypeTag] = typeMetadata(typeTag[T])._1

	def getWrapperType[T: TypeTag] = typeMetadata(typeTag[T])._2.tpe
	def getMirror[T: TypeTag] = typeMetadata(typeTag[T])._2.mirror
	def getConvertToBytes[T: TypeTag] = typeMetadata(typeTag[T])._3.asInstanceOf[T => Array[Byte]]
	def getConvertToObj[T: TypeTag] = typeMetadata(typeTag[T])._4.asInstanceOf[Array[Byte] => T]

//Returns each of the above tools in a set for efficiency if all are needed
//	._1 -> Data Type
//	._2 -> Wrapper Type
//	._3 -> Mirror
//	._4 -> Function literal for deconstructing the object into bytes
//	._5 -> Function literal for reconstructing the object from bytes
	def getReflectionTools(_type: String) = {val typeMeta = typeMetadata(_type: TypeTag[_]);(typeMeta._2.tpe, typeMeta._2.mirror, typeMeta._4)}

	def construct(_type: String, _data: Array[Byte]): SendableOld[_] =
	{
		val (objType, mirror, bytesToObj) = getReflectionTools(_type)
		val obj = mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asMethod)(bytesToObj(_data), null)

		objType match
		{
		case dt if dt =:= typeOf[SendableOld[String]] => obj.asInstanceOf[SendableOld[String]]
		case dt if dt =:= typeOf[SendableOld[Image]] => obj.asInstanceOf[SendableOld[Image]]
		case dt if dt =:= typeOf[SendableOld[Array[Byte]]] => obj.asInstanceOf[SendableOld[Array[Byte]]]
		case _ => obj.asInstanceOf[SendableOld[_]]
		}
	}
}

class SendableOld[T](_obj: T, _guid: UUID = null) extends Util
{import nx.comm.SendableOld._

	val guid = if (_guid != null) _guid else "blah" + "-" + UUID.randomUUID.toString.split("-", 2)(1): UUID

	val obj = _obj

//	def toBytes = getConvertToBytes[T](obj)
//
//	def getPackets(_num: Int): Array[SendableMetadata] =
//	{
//		val toReturn = new Array[SendableMetadata](_num)
//		val byteGroups = toBytes.grouped(_num).toArray
//		for (i <- 0 until _num)
//			toReturn(i) = SendableMetadata(guid, i, _num, byteGroups(i))
//		toReturn
//	}
}