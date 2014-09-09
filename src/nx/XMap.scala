package nx

import scala.language.implicitConversions
import scala.reflect.runtime.universe._

object XMap
{
	implicit def fromAny[V](_v: V): XPart[V] = new XPart[V] {val v = _v}
	def apply[V: TypeTag, X: TypeTag](_entries: XMapEntry[V, X]*) = XMap[V, X](_entries)
	trait XPart[V]
	{
		val v: V
		def <->[X](x: X): XMapEntry[V, X] = XMapEntry[V, X](v, x)
	}
	case class XMapEntry[V, X](v: V, x: X)
	case class XMap[V: TypeTag, X: TypeTag](_entries: Seq[XMapEntry[V, X]])
	{
		def apply[T: TypeTag](_obj: T, _firstOnly: Boolean = false) = typeOf[T] match
			{
				case t if t <:< typeOf[V] && t <:< typeOf[X] => _entries.find(_.v == _obj).map(_.x).getOrElse(_entries.find(_.x == _obj).map(_.v).get)
				case t if t <:< typeOf[V] => _entries.find(_.v == _obj).map(_.x).get
				case t if t <:< typeOf[X] => _entries.find(_.x == _obj).map(_.v).get
				case _ => null
			}
	}
}