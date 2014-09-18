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
	def getReflectionTools[T: TypeTag] =
	{
		val typeMeta = typeMetadata(typeTag[T])
		(typeMeta._1.tpe, typeMeta._1.mirror, typeMeta._3.asInstanceOf[Array[Byte] => T])
	}
	def convertToBytes[T: TypeTag](_obj: T) = typeMetadata(typeTag[T])._2.asInstanceOf[T => Array[Byte]](_obj)

	private val typeIDIndex_ = XMap[String, TypeTag[_]](
		"00000001" <-> typeTag[String],
		"00000002" <-> typeTag[Image],
		"00000003" <-> typeTag[Array[Byte]]
	)
	def typeIDInterface(_str: String) = typeIDIndex_(_str).asInstanceOf[TypeTag[_]]
	def typeIDInterface[T: TypeTag] = typeIDIndex_(typeTag[T]).asInstanceOf[String]
	def typeFromGUID(_guid: UUID) = typeIDInterface(_guid.toString.split("-", 2)(0))

	def construct(_partials: Array[SendableMetadata]): Sendable[_] = construct(_partials(0).guid, _partials.map(_.data).flatten)
	def construct(_guid: UUID, _data: Array[Byte]): Sendable[_] =
	{
		val tag: TypeTag[_] = typeFromGUID(_guid)
		val (objType, mirror, bytesToObj) = getReflectionTools(tag)
		mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asMethod)(bytesToObj(_data), _guid, tag).asInstanceOf[Sendable[_]]
	}
}

class Sendable[T](_obj: T, _guid: UUID = null)(implicit tag: TypeTag[T]) extends Tools
{import Sendable._
	var obj = _obj
	var guid = if (_guid != null) _guid else typeIDInterface(tag) + "-" + UUID.randomUUID.toString.split("-", 2)(1): UUID
	def toBytes = convertToBytes[T](obj)
	def getPackets(_num: Int): Array[SendableMetadata] =
	{
		val bytes = toBytes
		bytes.grouped(bytes.length / _num).toArray.zipWithIndex.map{case(data, i) => SendableMetadata(guid, i + 1, _num, data)}
	}
	def isType[T: TypeTag] = typeOf[T] =:= tag.tpe
	def getType = tag.tpe

	override def toString = s"Sendable[${regex("(?:[^\\.]*)$", tag.tpe)}]\nGUID: ${guid}\nData: ${if (isType[String]) obj else "<data not displayable>"}"

//	<Dev tools>
	def toFile(_filePath: String): Unit = toFile(_filePath, getPackets(10).mkString("\r\n"))
//	</Dev tools>
}