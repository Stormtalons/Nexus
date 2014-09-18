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
	private val typeMetadata = HashMap[Type, (TypeTag[_], _ => Array[Byte], Array[Byte] => _)](
		typeOf[java.lang.String] -> (
			typeTag[Sendable[String]],
			(_str: String) => _str: Array[Byte],
			(_data: Array[Byte]) => _data: String
		),

		typeOf[Image] -> (
			typeTag[Sendable[Image]],
			(_img: Image) => _img: Array[Byte],
			(_data: Array[Byte]) => _data: Image
		),

		typeOf[RemoteFile] -> (
			typeTag[Sendable[RemoteFile]],
			(_fileData: RemoteFile) => _fileData.fileName: Array[Byte],
			(_data: Array[Byte]) => new RemoteFile(_data: String)
		)
	)
	def getReflectionTools(_type: Type) =
	{
		val typeMeta = typeMetadata(_type)
		(typeMeta._1.tpe, typeMeta._1.mirror, typeMeta._3.asInstanceOf[Array[Byte] => _type.type])
	}

	private val typeIDIndex_ : XMap[String, Type] = XMap[String, Type](
		"00000001" <-> typeOf[java.lang.String],
		"00000002" <-> typeOf[Image],
		"00000003" <-> typeOf[RemoteFile]
	)
	def typeFromGUID(_guid: UUID) = typeIDIndex_.l(_guid.toString.split("-", 2)(0))

	def construct(_partials: Array[SendableMetadata]): Sendable[_] = construct(_partials(0).guid, _partials.map(_.data).flatten)
	def construct(_guid: UUID, _data: Array[Byte]): Sendable[_] =
	{
		val type_ = typeFromGUID(_guid)
		val (objType, mirror, bytesToObj) = getReflectionTools(type_)
		mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asMethod)(bytesToObj(_data), _guid, type_).asInstanceOf[Sendable[_]]
	}
}

class Sendable[T](_obj: T, _guid: UUID = null)(implicit tag: TypeTag[T]) extends Tools
{import Sendable._
	var obj = _obj
	var guid = if (_guid != null) _guid else typeIDIndex_(tag.tpe) + "-" + UUID.randomUUID.toString.split("-", 2)(1): UUID
	def toBytes = typeMetadata(typeOf[T])._2.asInstanceOf[T => Array[Byte]](obj)
	def getPackets(_num: Int): Array[SendableMetadata] =
	{
		val bytes = toBytes
		bytes.grouped(bytes.length / _num).toArray.zipWithIndex.map{case(data, i) => SendableMetadata(guid, i + 1, _num, data)}
	}
	def isType[A: TypeTag] = typeOf[A] =:= typeOf[T]
	def getType = typeOf[T]
	override def toString = s"Sendable[${regex("(?:[^\\.]*)$", tag.tpe)}]\nGUID: ${guid}\nData: ${if (isType[String]) obj else "<data not displayable>"}"
}