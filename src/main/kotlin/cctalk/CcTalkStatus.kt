package cctalk

enum class CcTalkStatus {
  /**
   * Everything is fine.
   */
  Ok,

  /**
   * Invalid com port.
   */
  WrongCom,

  /**
   * Error opening com port.
   */
  OpenErr,

  /**
   * No device found while opening connection.
   */
  NoDevice,

  /**
   * Can't setup com port.
   */
  SetupErr,

  /**
   * Error closing com port.
   */
  CloseErr,

  /**
   * Error sending data block.
   */
  SendErr,

  /**
   * Port is already opened.
   */
  AlreadyOpen,

  /**
   * Port not open.
   */
  NotOpen,

  /**
   * Missing local echo - most likely a hardware problem.
   */
  BadLine,

  /**
   * Receive timeout.
   */
  RcvTimeout,

  /**
   * Internal library error.
   */
  Internal,

  /**
   * Faulty block format.
   */
  BlockFormat,

  /**
   * Data length error.
   */
  DataLen,

  /**
   * Common receive error.
   */
  ReceiveError,

  /**
   * Negative acknowledge received.
   */
  NoAck,

  /**
   * Wrong checksum.
   */
  ChSumErr,

  /**
   * Poll too slow - buffered events lost.
   */
  EventsLost,

  /**
   * Wrong data format in response.
   */
  DataFormat,

  /**
   * Wrong destination address.
   */
  WrongAddr,

  /**
   * Bad ref parameter.
   */
  WrongParameter,

  /**
   * Invalid parameter value.
   */
  InvalidParameter,

  /**
   * Invalid command.
   */
  InvalidCommand,

  /**
   * Bill route: Escrow was empty.
   */
  BillEscrowEmpty,

  /**
   * Bill route: Failed to route bill.
   */
  BillRouteFailed,

  /**
   * Command not valid for this device.
   */
  WrongCommand,

  /**
   * Device not supported.
   */
  UnSupported,

  /**
   * Payout time exceeded.
   */
  PayoutExceeded,

  /**
   * A clone file was not loaded.
   */
  FileNotLoaded,

  /**
   * The clone file wasn't compatible with the actual device.
   */
  FileNotCompatible,

  /**
   * A lengthy operation was cancelled by the main application.
   */
  OperationCancelled,

  /**
   * Short circuit on serial port.
   */
  SerialShortCircuit,

  /**
   * Failed to initialise CCT 910 for serial communication.
   */
  SerialInitFailure,

  /**
   * Exception while initialising CCT 910 for serial communication.
   */
  SerialInitException,

  /**
   * Error while initialising DES encryption.
   */
  InitEncryption,

  /**
   * Wrong DES key length.
   */
  DESKeyLength,

  /**
   * Error while decrypting DES receive data.
   */
  Decryption,

  /**
   * Error while encrypting DES send data.
   */
  Encryption,

  /**
   * Unsupported encryption method.
   */
  UnsupportedEncryption,

  /**
   * A ID003 comm error was reported.
   */
  CommError,

  /**
   * The TWS 100 Escrow Sorter has rejected the cash command.
   */
  CommandRejected,

  /**
   * The TWS 100 Escrow Sorter has returned a wrong cash reference number.
   */
  CommandSequence,

  /**
   * The command cannot be applied to this device.
   */
  WrongDevice,

  /**
   * Nobody knows...
   */
  Unknown;

  fun isError(): Boolean = this != Ok

  fun isOk(): Boolean = this == Ok

  fun isNotOk(): Boolean = this != Ok
}