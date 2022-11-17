package com.relayfeeder.kma.driver;

public class CommonUsbSerialPortWrapper {
    public static byte[] getWriteBuffer(UsbSerialPort serialPort) {
        CommonUsbSerialPort commonSerialPort = (CommonUsbSerialPort) serialPort;
        return commonSerialPort.mWriteBuffer;
    }
}
