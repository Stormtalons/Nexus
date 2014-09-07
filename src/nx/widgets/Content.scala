package nx.widgets

trait Content[T >: Null]
{
	protected var content_ : T = null
	def content = content_
	def content_=(_content: T) = content_ = _content
	def getContent: Array[Byte]
}
