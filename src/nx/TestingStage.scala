package nx

import java.nio.file.Paths
import javafx.scene.image.Image

import nx.comm.ConnectionManager
import nx.comm.sendable.{Sendable}
import nx.util.{Tools}

import scala.language.postfixOps

object TestingStage extends App with Tools
{
	val s = new Sendable("hi there")
	println(s.isType[String])
}