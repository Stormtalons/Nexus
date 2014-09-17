package nx.comm.sendable

import java.util.UUID
import javafx.scene.image.Image

import nx.util.XMap._
import nx.util.{Tools, XMap}

import scala.collection.immutable.HashMap
import scala.language.{existentials, implicitConversions, postfixOps}
import scala.reflect.runtime.universe._

object Sendable extends Tools
{
	private val typeMetadata = HashMap[TypeTag[_], (TypeTag[_], _ => Array[Byte], Array[Byte] => _)](
		typeTag[String] -> (
			typeTag[Sendable[String]],
			(_str: String) => _str: Array[Byte],
			(_data: Array[Byte]) => _data: String
		),

		typeTag[Image] -> (
			typeTag[Sendable[Image]],
			(_img: Image) => _img: Array[Byte],
			(_data: Array[Byte]) => _data: Image
		),

		typeTag[Array[Byte]] -> (
			typeTag[Sendable[Array[Byte]]],
			(_fileData: Array[Byte]) => _fileData,
			(_data: Array[Byte]) => _data
		)
	)
	def getReflectionTools[T](_type: TypeTag[T]) =
	{
		val typeMeta = typeMetadata(_type)
		(typeMeta._1.tpe, typeMeta._1.mirror, typeMeta._3.asInstanceOf[Array[Byte] => T])
	}
	def convertToBytes[T](_obj: T, _type: TypeTag[T]) = typeMetadata(_type)._2.asInstanceOf[T => Array[Byte]](_obj)

	private val typeIDIndex_ = XMap[String, TypeTag[_]](
		"00000001" <-> typeTag[String],
		"00000002" <-> typeTag[Image],
		"00000003" <-> typeTag[Array[Byte]]
	)
	def typeIDInterface(_str: String) = typeIDIndex_(_str).asInstanceOf[TypeTag[_]]
	def typeIDInterface(_tag: TypeTag[_]) = typeIDIndex_(_tag).asInstanceOf[String]
	def typeFromGUID(_guid: UUID) = typeIDInterface(_guid.toString.split("-", 2)(0))

	def construct(_guid: UUID, _data: Array[Byte]) =
	{
		val tag = typeFromGUID(_guid)
		val (objType, mirror, bytesToObj) = getReflectionTools(tag)
		mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asMethod)(bytesToObj(_data), tag, null).asInstanceOf[Sendable[_]]
	}
}

class Sendable[T](_obj: T, _tag: TypeTag[T], _guid: UUID = null) extends Tools
{import Sendable._
	val obj = _obj
	val guid = if (_guid != null) _guid else typeIDInterface(_tag) + "-" + UUID.randomUUID.toString.split("-", 2)(1): UUID
	def toBytes = convertToBytes(obj, _tag)
	override def toString = s"Sendable[${regex("(?:[^\\.]*)$", _tag.tpe)}]\nGUID: ${guid}\nData: ${if (_obj.isInstanceOf[String]) _obj else "<data not displayable>"}"
	def getPackets(_num: Int): Array[SendableMetadata] =
	{
//		val toReturn = new Array[SendableMetadata](_num)
//		val byteGroups = toBytes.grouped(_num).toArray.zipWithIndex.map{case(data, i) => SendableMetadata(guid, i + 1, _num, data)}
//		byteGroups.zipWithIndex.foreach{case(data, i) => toReturn(i) = SendableMetadata(guid, i + 1, _num, data)}
//		(0 until _num).foreach(i => toReturn(i) = SendableMetadata(guid, i + 1, _num, byteGroups(i)))
//		toReturn
		toBytes.grouped(_num).toArray.zipWithIndex.map{case(data, i) => SendableMetadata(guid, i + 1, _num, data)}
	}
}

