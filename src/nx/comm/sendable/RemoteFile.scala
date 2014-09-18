package nx.comm.sendable

import java.io.File
import java.nio.file.{Paths, Files}

import nx.util.Tools

class RemoteFile(_fileName: String, _fileData: Array[Byte] = Array(), _hasFileData: Boolean = false) extends Tools
{
	def this(_filePath: String) = this(Paths.get(_filePath).getFileName.toString, Files.readAllBytes(Paths.get(_filePath)))

	//TODO: Make this lazy.
	def this(_file: File) = this(_file.getName, Files.readAllBytes(Paths.get(_file.getPath)), true)

	var fileName = _fileName
	protected var fileData_ = _fileData
	def fileData = fileData_
	def fileData_=(_fileData: Array[Byte]) =
	{
		fileData_ = _fileData
		hasFileData = true
	}
	var hasFileData = _hasFileData
}