package edu.usc.officeshare.signal;

import org.alljoyn.bus.annotation.Position;

public class FileInfo {
	@Position(0)
	public String fileServerIP; //signature is s
	
	@Position(1)
	public int fileServerPort; //signature is q (unsigned int32)
	
	@Position(2)
	public String fileName;
	
	@Position(3)
	public long fileSize; //signature is t (unsigned int64)
	
	public FileInfo(){
		fileServerIP = "0.0.0.0";
		fileServerPort = 0;
		fileName = "";
		fileSize = 0;		
	}
	
	public FileInfo(String IP, int port, String name, long size){
		fileServerIP = IP;
		fileServerPort = port;
		fileName = name;
		fileSize = size;
	}
	
	public FileInfo(FileInfo fi){
		fileServerIP = fi.fileServerIP;
		fileServerPort = fi.fileServerPort;
		fileName = fi.fileName;
		fileSize = fi.fileSize;
	}
	
	
}
