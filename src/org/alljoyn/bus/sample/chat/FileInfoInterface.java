package org.alljoyn.bus.sample.chat;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusProperty;

@BusInterface (name = "org.alljoyn.bus.FileInfo")
public interface FileInfoInterface {

	@BusProperty
    public FileInfo getFileInfo() throws BusException;
	
}
