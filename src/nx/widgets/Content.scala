package nx.widgets

import nx.comm.sendable.Sendable

trait Content[T]
{
	protected var content: Sendable[T]
}
