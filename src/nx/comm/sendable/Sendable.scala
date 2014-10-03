package nx.comm.sendable

import java.util.UUID
import javafx.scene.image.Image

import nx.util.{Tools}
import sw.common.XMap

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
	private val typeIDIndex_ : XMap[String, TypeTag[_]] = XMap[String, TypeTag[_]](
		"00000001" <-> typeTag[java.lang.String],
		"00000002" <-> typeTag[Image],
		"00000003" <-> typeTag[RemoteFile]
	)

	def getReflectionTools(_type: Type) =
	{
		val typeMeta = typeMetadata(_type)
		(typeMeta._1.tpe, typeMeta._1.mirror, typeMeta._3.asInstanceOf[Array[Byte] => _type.type])
	}
	def typeFromGUID(_guid: UUID) = typeIDIndex_.l(_guid.toString.split("-", 2)(0))
	def typeToGUID[T: TypeTag] = UUID.fromString(Sendable.typeIDIndex_.r(typeTag[T]) + "-" + UUID.randomUUID.toString.split("-", 2)(1))

	def apply[T: TypeTag](_obj: T): Sendable[T] = Sendable(_obj, UUID.fromString(Sendable.typeIDIndex_.r(typeTag[T]) + "-" + UUID.randomUUID.toString.split("-", 2)(1)))
	def construct(_partials: Array[SendableMetadata]): Sendable[_] = construct(_partials(0).guid, _partials.map(_.data).flatten)
	def construct(_guid: UUID, _data: Array[Byte]): Sendable[_] =
	{
		val tag = typeFromGUID(_guid)
		val (objType, mirror, bytesToObj) = getReflectionTools(tag.tpe)
		mirror.reflectClass(mirror.classSymbol(mirror.runtimeClass(objType))).reflectConstructor(objType.member(stringToTermName("<init>")).asMethod)(bytesToObj(_data), _guid, tag).asInstanceOf[Sendable[_]]
	}
}

case class Sendable[T](obj: T, guid: UUID)(implicit tag: TypeTag[T]) extends Tools
{import Sendable._
	def toBytes = typeMetadata(typeOf[T])._2.asInstanceOf[T => Array[Byte]](obj)
	def getPackets(_num: Int): Array[SendableMetadata] =
	{
		val bytes = toBytes
		bytes.grouped(math.ceil(bytes.length / _num.toDouble).toInt).toArray.zipWithIndex.map{case(data, i) => SendableMetadata(guid, i + 1, _num, data)}
	}
	def isType[A: TypeTag] = typeOf[A] =:= typeOf[T]
	def getType = typeOf[T]
	override def toString = s"Sendable[${regex("(?:[^\\.]*)$", tag.tpe)}]\nGUID: ${guid}\nData: ${if (isType[String]) obj else "<data not displayable>"}"
}