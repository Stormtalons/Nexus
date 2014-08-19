package nx

import scala.collection.mutable.ArrayBuffer

object JSON
{
	val OBJECT = 0
	val STRING = 1
	val ARRAY = 2

	def condense(_str: String): String =
	{
		val sb = new StringBuilder
		var quoteCount = 0
		for (i <- 0 to _str.length - 1)
			_str.charAt(i) match
			{
				case '\"' =>
					quoteCount += 1
					sb.append(_str.charAt(i))
				case '\r' => if (quoteCount % 2 != 0) sb.append(_str.charAt(i))
				case '\n' => if (quoteCount % 2 != 0) sb.append(_str.charAt(i))
				case '\t' => if (quoteCount % 2 != 0) sb.append(_str.charAt(i))
				case ' ' => if (quoteCount % 2 != 0) sb.append(_str.charAt(i))
				case _ => sb.append(_str.charAt(i))
			}

		sb.toString
	}

	def find(str: String, char: Char, opposites: (Char, Char)*): Int =
	{
		var charCount = 0
		var quoteCount = 0
		for (i <- 1 to str.length - 1)
		{
			opposites.foreach(o => charCount += (if (str.charAt(i) == o._1) 1 else if (str.charAt(i) == o._2) -1 else 0))
			str.charAt(i) match
			{
			case '\"' =>
				if (char == '\"' && quoteCount % 2 != 0 && charCount == 0)
					return i
				quoteCount += 1
			case `char` => if (quoteCount % 2 != 0 && charCount == 0) return i
			case _ =>
			}
		}
		str.length
	}
	
	def parse(_str: String): JSON =
	{
		val toReturn = new JSON
		var str = condense(_str)

		if (str.charAt(0) == '\"')
		{
			toReturn.label = str.substring(1, find(str, ':') - 1)
			str = str.drop(find(str, ':') + 1)
			if (str.charAt(0) == '\"')
			{
				toReturn.value = str.substring(1, find(str, '\"') - 1)
				toReturn.datatype = STRING
			}
			else if (str.charAt(0) == '[')
			{
				val array = new ArrayBuffer[JSON]
				var arrayStr = str.substring(1, find(str, ']', ('[', ']')) - 1)
				str = str.drop(arrayStr.length)
				while (arrayStr.length > 0)
				{
					val segment = find(arrayStr, ',', ('{', '}'), ('[', ']'))
					array += parse(arrayStr.substring(0, segment))
					arrayStr = arrayStr.drop(segment + 1)
				}

				toReturn.value = array
				toReturn.datatype = ARRAY
			}
			else if (str.charAt(0) == '{')
			{
				val matching = find(str, '}', ('{', '}'))
				toReturn.value = parse(str.substring(0, matching))
				str = str.drop(matching + 1)
			}
		}
		else if (str.charAt(0) == '{')
		{
			var objectStr = str.substring(1, find(str, '}', ('{', '}')) - 1)
			str = str.drop(objectStr.length)
			while (objectStr.length > 0)
			{
				val segment = find(objectStr, ',', ('{', '}'), ('[', ']'))
				toReturn.+=(parse(objectStr.substring(0, segment)))
				objectStr = objectStr.drop(segment + 1)
			}
		}

		toReturn
	}
}

case class JSON(_label: Option[String] = Some(""), _value: Option[AnyRef] = None)
{
	implicit def stringToOption(s: String) = Some(s)
	implicit def anyrefToOption(ar: AnyRef) = Some(ar)

	private var datatype_ : Int = try _value.get match
	{
		case str: String => JSON.STRING
		case json: JSON => JSON.OBJECT
		case ar: ArrayBuffer[JSON] => JSON.ARRAY
	}
	catch {case e => JSON.OBJECT}
	def datatype = datatype_
	def datatype_= (_datatype: Int) = datatype_ = _datatype

	private var label_ : String = _label.getOrElse[String]("")
	def label = label_
	def label_= (_label: String) = label_ = _label

	private var value_ : AnyRef = _value.orNull
	def value = value_
	def value_= (_value: AnyRef) = value_ = _value match
	{
		case str: String => str
		case json: JSON => json.clone
		case ar: ArrayBuffer[JSON] =>
			var newArray = new ArrayBuffer[JSON]
			ar.foreach(json => newArray = newArray :+ json.clone)
			newArray
		case _ => null
	}

	private val values_ : ArrayBuffer[JSON] = new ArrayBuffer[JSON]
	def values = values_
	def values_= (_values: ArrayBuffer[JSON]) =
	{
		values.clear
		_values.foreach(_json => +=(_json.clone))
	}

	private var indent_ = ""
	def indentStr: String = indent_
	def indent: Int = indentStr.length
	def indent_= (amt: Int) =
	{
		indent_ = ""
		for (i <- 0 to amt - 1)
			indent_ += "\t"
	}
	
	def +=(_key: String, _val: AnyRef): Unit = +=(JSON(_key, _val))
	def +=(_json: JSON): Unit = values += _json
	def get(i: Int): AnyRef =
		if (i < values.length)
			values(i).value
		else
			null
	def get(key: String): AnyRef =
	{
		values.foreach(v => if (v.label.equals(key)) return v.value)
		null
	}

	override def toString: String =
	{
		val toReturn = new StringBuilder

		if (label.length > 0)
			toReturn.append(indentStr + "\"" + label + "\" : ")

		if (datatype == JSON.STRING)
			toReturn.append("\"" + value + "\"")
		else if (datatype == JSON.OBJECT && value != null)
		{
			value.asInstanceOf[JSON].indent = indent
			toReturn.append(value.toString)
		}
		else
		{
			val (values, prefix, postfix) = if (datatype == JSON.OBJECT) (this.values, indentStr + "{", "}") else (value.asInstanceOf[ArrayBuffer[JSON]], "[", "]")
			toReturn.append(prefix + "\n")
			for (i <- 0 to values.length - 1)
			{
				values(i).indent = indent + 1
				toReturn.append(values(i).toString)
				if (i < values.length - 1)
					toReturn.append(",")
				toReturn.append("\n")
			}
			toReturn.append(indentStr + postfix)
		}

		toReturn.toString
	}

	override def clone: JSON =
	{
		val toReturn = JSON(label, value)
		toReturn.datatype = datatype
		toReturn.values = values
		toReturn
	}
}
