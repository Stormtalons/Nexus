package nx.comm

import javafx.scene.image.Image

object Sendable
{
	val STRING: Byte = 0
	val IMAGE: Byte = 1
	val FILE: Byte = 2
}

trait Sendable[T <: Sendable[T]]
{import nx.Main._
	def label: String
	def bytes: Array[Byte]
	def obj: AnyRef
	def splitLabel(_bytes: Array[Byte]): (String, Array[Byte]) =
	{
		var i = 0
		while (i < _bytes.length && _bytes(i) != '|')
			i += 1
		val (imgLabel, imgData) = _bytes.splitAt(i)
		(imgLabel, imgData)
	}
}

class SendableString(_bytes: Array[Byte]) extends Sendable[SendableString]
{import nx.Main._
	def label = ""
	def bytes: Array[Byte] = Sendable.STRING + obj
	def obj: String = _bytes
}

class SendableImage(_bytes: Array[Byte]) extends Sendable[SendableImage]
{import nx.Main._
	val parts = splitLabel(_bytes)
	def label = parts._1
	def bytes = (Sendable.IMAGE + label + "|": Array[Byte]) union parts._2
	def obj: Image = parts._2
}

class SendableFile(_bytes: Array[Byte]) extends Sendable[SendableFile]
{import nx.Main._
	val parts = splitLabel(_bytes)
	def label = parts._1
	def bytes = (Sendable.FILE + label + "|": Array[Byte]) union parts._2
	def obj: Array[Byte] = parts._2
}