package nx

import java.net.{Socket, InetSocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, SocketChannel}
import java.util.Base64
import javafx.application.Application
import javafx.event.{ActionEvent, EventHandler}
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.{TextField, Separator, Button, TextArea}
import javafx.scene.layout.{Priority, VBox}
import javafx.stage.{WindowEvent, Stage}

import nx.util.{Asynch, InterfaceShortcuts, Tools}

import scala.language.postfixOps

object TestingStage extends App with Tools with Asynch
{
	var in: TextArea = null
	var socket: Socket = null

	addActivity({
		var data = new Array[Byte](socket.getInputStream.available)
		val read = socket.getInputStream.read(data)
		if (read == -1)
			stop
		else if (read > 0)
		{
			if (read < data.length)
				data = data.splitAt(read)._1
			in.setText(new String(data)).fx
		}
		Thread.sleep(100)
	})^^

	addCallback(socket.close.^^)
	new TestingStage().launch
}

class TestingStage extends Application with Tools with InterfaceShortcuts
{
	def launch = javafx.application.Application.launch()

	def start(_stg: Stage) =
	{
		TestingStage.in = new TextArea
		VBox.setVgrow(TestingStage.in, Priority.ALWAYS)
		val ipt1 = "TRACE / HTTP/1.1\nConnection: keep-alive\nCache-Control: public\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\nUser-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36\nAccept-Encoding: gzip,deflate,sdch\nAccept-Language: en-US,en;q=0.8\nAccept-Ranges: bytes\n\n"
		val ipt2 = "DRV_GET_MY_NODE_INFO"
		val soap =
"""

<soap:Envelope
 xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
 xmlns:cwmp="urn:dslforum-org:cwmp-1-2">
 <soap:Header>
  <cwmp:ID soap:mustUnderstand="1">CSJD8179202520</cwmp:ID>
 </soap:Header>
 <soap:Body>
  <cwmp:GetRPCMethods />
 </soap:Body>
</soap:Envelope>

""".trim

		val ipt3 =
		s"""|GET / HTTP/1.1
			|Content-Type: text/xml; charset="utf-8"
   			|Authorization: Basic ${Base64.getEncoder.encodeToString("admin:bridgett1": Array[Byte])}
			|Content-Length: ${soap.length}
   			|
   			|""".stripMargin + soap

		val out = new TextArea(ipt3)
		VBox.setVgrow(out, Priority.ALWAYS)
		val addr = new TextField("71.170.100.10:4567")
		val sep = new Separator
		sep.setOrientation(Orientation.HORIZONTAL)
		val send = new Button("Connect and Send")
		send.setOnAction(
			{
				TestingStage.socket.close.^^
				TestingStage.stopAndWait
				TestingStage.socket = new Socket
				TestingStage.socket.connect(new InetSocketAddress(addr.getText.split(":")(0), addr.getText.split(":")(1).toInt)).^^#
				TestingStage.run
				Thread.sleep(100)
				TestingStage.socket.getOutputStream.write(out.getText: Array[Byte])
				TestingStage.socket.getOutputStream.flush
			}.x)
		val v = new VBox
		v.setSpacing(15)
		v.getChildren.addAll(out, addr, send, sep, TestingStage.in)
		_stg.addEventFilter(WindowEvent.WINDOW_HIDING, System.exit(0): EventHandler[WindowEvent])
		_stg.setScene(new Scene(v, 1000, 1000))
		_stg.show
	}
}