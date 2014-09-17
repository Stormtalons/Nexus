package nx.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.nio.channels.spi.SelectorProvider
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.{Calendar, UUID}
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.stage.Window
import javax.imageio.ImageIO

import nx.Main
import nx.widgets.FolderWidget

import scala.language.{implicitConversions, postfixOps}
import scala.util.control.Breaks._

trait Tools
{
	implicit def codeToCode[T <: Any](_code: => T) = new Code(_code)

	case class Repeat(_val: Any){def ^(_i: Int): String = {var s = "";for (i <- 0 until _i) s += _val.toString;s}}
	implicit def toRpt(_val: Byte) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Int) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Long) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Float) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Double) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: Char) = Repeat(_val.asInstanceOf[Any])
	implicit def toRpt(_val: String) = Repeat(_val.asInstanceOf[Any])

	case class Screen[T <: Any](_p: T)
	{
		def ^# : T = ^#(1)
		def ^#(_crlf: Int) : T = {print(_p + ("\n"^_crlf));_p}
		def ^@ : T = {print(_p);_p}
	}
	implicit def anyToScreen[T <: Any](_p: T): Screen[T] = Screen(_p)

	def serverPort = Main.serverPort_
	def serverPort_=(_port: Int) = Main.serverPort_ = _port
	def bufferSize = Main.bufferSize_
	def bufferSize_=(_size: Int) = Main.bufferSize_ = _size
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
	def logWriter = Main.logWriter_
	def log(_str: String, _lines: Int = 1): Unit = {println(_str);logWriter.write(s"Instance ${instance} - ${new SimpleDateFormat("yyyyMMdd.hh:mm:ss").format(Calendar.getInstance.getTime)}: ${_str}${"\r\n"^_lines}").^^*}
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

	implicit def bytesToStr(_bytes: Array[Byte]): String = new String(_bytes, "UTF-8")
	implicit def bytesToImg(_bytes: Array[Byte]): Image = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(_bytes)), null)

	implicit def strToBytes(_str: String): Array[Byte] = _str.getBytes("UTF-8")
	implicit def strToPath(_str: String): Path = Paths.get(_str)
//	implicit def strToJSON(_str: String): JSON = JSON.parse(_str)
	implicit def strToUUID(_str: String): UUID = UUID.fromString(_str)
	implicit def strToImg(_str: String): Image = SwingFXUtils.toFXImage(ImageIO.read(getClass.getResource(_str)), null).asInstanceOf[Image]

	implicit def pathToBytes(_path: Path): Array[Byte] = if (fileExists(_path)) Files.readAllBytes(_path) else Array[Byte]()
	implicit def pathToStr(_path: Path): String = _path: Array[Byte]

	implicit def UUIDToBytes(_uuid: UUID): Array[Byte] = _uuid.toString: Array[Byte]
	implicit def UUIDToStr(_uuid: UUID): String = _uuid.toString

	implicit def fileToBytes(_file: File): Array[Byte] = if (fileExists(_file)) _file: Path else Array[Byte]()
	implicit def fileToStr(_file: File): String = if (fileExists(_file)) _file: Array[Byte] else ""
	implicit def fileToPath(_file: File): Path = if (fileExists(_file)) _file.getPath: Path else null

	implicit def imgToBytes(_img: Image): Array[Byte] =
	{
		val baos = new ByteArrayOutputStream
		ImageIO.write(SwingFXUtils.fromFXImage(_img, null), "jpg", baos)
		baos.toByteArray
	}

	def fileExists(_file: File): Boolean = _file != null && _file.exists
	def fileExists(_path: Path): Boolean = Files.exists(_path)
	def fileExists(_filePath: String): Boolean = fileExists(_filePath: Path)
	def fileText(_filePath: String): String = (_filePath: Path): Array[Byte]
	def toFile(_filePath: String, _bytes: Array[Byte]): Unit = Files.write(_filePath: Path, _bytes)
	def toFile(_filePath: String, _str: String): Unit = toFile(_filePath, _str: Array[Byte])

	def indexOf(_bytes: Array[Byte], _toFind: Array[Byte]): Int =
	{
		for (i <- 0 until _bytes.length - _toFind.length)
			breakable({
				for (j <- 0 until _toFind.length)
					if (_bytes(i + j) != _toFind(j))
						break
				return i
			})
		-1
	}

	def tryGet[T <: Any](_code: => T, _dft: => T = null): T = try _code catch {case e: Exception => _dft}
	def regex[T <: Any](_regex: String, _input: T) =
	{
		val m = Pattern.compile(_regex).matcher(_input.toString)
		if (m.find) m.group else ""
	}
}
