package nx.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.net.InetSocketAddress
import java.nio.channels.spi.SelectorProvider
import java.util.UUID
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.stage.Window
import javax.imageio.ImageIO

import akka.actor.{ActorSystem, Actor, ActorRef}
import akka.io.Tcp.Connect
import nx.Main
import nx.comm.O
import nx.comm.sendable.{RemoteFile, Sendable}
import nx.widgets.FolderWidget

import scala.collection.mutable
import scala.language.{implicitConversions, postfixOps}

trait Tools extends sw.common.Tools
{
	def serverPort = Main.serverPort_
	def serverPort_=(_port: Int) = Main.serverPort_ = _port
	def bufferSize = Main.bufferSize_
	def bufferSize_=(_size: Int) = Main.bufferSize_ = _size
	def receivedItems = Main.receivedItems_
	def receivedItems_=(_receivedItems: mutable.HashMap[UUID, Sendable[_]]) = Main.receivedItems_ = _receivedItems
	def createChannel = SelectorProvider.provider.openSocketChannel
	def createServerChannel = SelectorProvider.provider.openServerSocketChannel
	val sep = "<@@>"
	val eom = sep + "EM"
	def eom(_i: Int): Char = eom.charAt(_i)

//	<Dev tools>
	def useConfig = Main.useConfig_
	def useConfig_=(_config: Boolean) = Main.useConfig_ = _config
	def instance = Main.instance_
	def instance_=(_instance: Int) = Main.setLogFile(s"log${Main.instance_ = _instance;_instance}.log")
	implicit def logWriter = Main.logWriter_
//	</Dev tools>

	def window = Main.window_
	def window_=(_window: Window) = Main.window_ = _window
	def desktop = Main.desktop_
	def desktop_=(_desktop: FolderWidget) =
	{
		if (desktop != null)
			desktopPanel.getChildren.remove(desktop)
		Main.desktop_ = _desktop
		desktopPanel.getChildren.add(0, desktop)
		AnchorPane.setLeftAnchor(desktop, 0d)
		AnchorPane.setRightAnchor(desktop, 0d)
		AnchorPane.setTopAnchor(desktop, 0d)
		AnchorPane.setBottomAnchor(desktop, 0d)
		desktop.confirmConstraintsFor(5, 10)
		desktop.expand(false)
		desktop.setPrefSize(1024, 768)
	}
	def desktopPanel = Main.desktopPanel_
	def desktopPanel_=(_desktopPanel: AnchorPane) = Main.desktopPanel_ = _desktopPanel

	implicit def bytesToImg(_bytes: Array[Byte]): Image = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(_bytes)), null)

	implicit def strToUUID(_str: String): UUID = UUID.fromString(_str)
	implicit def strToImg(_str: String): Image = SwingFXUtils.toFXImage(ImageIO.read(getClass.getResource(_str)), null).asInstanceOf[Image]
	implicit def strToSendable(_str: String): Sendable[String] = new Sendable[String](_str, Sendable.typeToGUID[java.lang.String])

	implicit def UUIDToBytes(_uuid: UUID): Array[Byte] = _uuid.toString: Array[Byte]
	implicit def UUIDToStr(_uuid: UUID): String = _uuid.toString

	implicit def fileToSendable(_file: File): Sendable[RemoteFile] = new Sendable[RemoteFile](new RemoteFile(_file), Sendable.typeToGUID[RemoteFile])

	implicit def imgToBytes(_img: Image): Array[Byte] =
	{
		val baos = new ByteArrayOutputStream
		ImageIO.write(SwingFXUtils.fromFXImage(_img, null), "jpg", baos)
		baos.toByteArray
	}
}