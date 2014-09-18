package nx

import java.nio.file.Paths

import nx.comm.ConnectionManager
import nx.comm.sendable.{Sendable}
import nx.util.{Tools}

import scala.language.postfixOps

object TestingStage extends App with Tools
{
	println(Paths.get("K:/Temp/crypt.txt").getFileName)
}