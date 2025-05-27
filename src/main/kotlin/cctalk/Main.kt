package cctalk

import cctalk.device.ValidatorDevice
import cctalk.serde.CcTalkSerializerImpl
import cctalk.serial.CcTalkPortImpl
import cctalk.serial.ConcurrentSerialPort
import cctalk.validator.ValidatorBillRoute
import com.fazecast.jSerialComm.SerialPort

suspend fun main() {
    println("CCTalk Validator Example")
    val serialPort = ConcurrentSerialPort(
        port = SerialPort.getCommPort("/dev/tty.usbserial-whCCT1478159"), // Adjust the port as necessary
        localEcho = true,
        name = "cctalk",
        index = 0,
        communicationDelay = 25,
        timeOut = 200
    )
    serialPort.open().let {
        if (!it) {
            throw RuntimeException("Failed to open port")
        }
    }
    val port = CcTalkPortImpl(port = serialPort, serializer = CcTalkSerializerImpl())

    // This is where you would typically initialize your validator and start processing bills.
    // For example:
    val validator = ValidatorDevice(port)
    validator.setMasterInhibit(false)
    validator.setBillInhibit(true)

    while (true) {
        validator.pollValidator().map {
            if(it.events > 0) {
                it.responses.forEach { response -> println(response) }
            }
        }
        if(validator.billInEscrow) {
            validator.routeBill(ValidatorBillRoute.Stack)
        }
    }

    // Placeholder for actual implementation
    println("Implement your CCtalk validator logic here.")
}