package nx.util

import scala.language.implicitConversions
import scala.reflect.runtime.universe._

object XMap
{
	implicit def fromAny[V: TypeTag](_v: V): XPart[V] = new XPart[V] {val v = _v}
	def apply[V: TypeTag, X: TypeTag](_entries: XMapEntry[V, X]*) = new XMap[V, X](_entries)
}
abstract class XPart[V: TypeTag]
{
	val v: V
	def <->[X: TypeTag](x: X): XMapEntry[V, X] = XMapEntry[V, X](v, x)
}
case class XMapEntry[V: TypeTag, X: TypeTag](v: V, x: X)
class XMap[V: TypeTag, X: TypeTag](_entries: Seq[XMapEntry[V, X]])
{
	def apply[T: TypeTag](_obj: T) = typeTag[T] match
	{
		case t if t.tpe =:= typeOf[V] && t.tpe =:= typeOf[X] => _entries.find(_.v == _obj).map(_.x).getOrElse(_entries.find(_.x == _obj).map(_.v).get)
		case t if t.tpe =:= typeOf[V] => _entries.find(_.v == _obj).map(_.x).get
		case t if t.tpe =:= typeOf[X] => _entries.find(_.x == _obj).map(_.v).get
		case _ => null
	}
	def l(_obj: V): X = _entries.find(_.v == _obj).map(_.x).get
	def r(_obj: X): V = _entries.find(_.x == _obj).map(_.v).get
}