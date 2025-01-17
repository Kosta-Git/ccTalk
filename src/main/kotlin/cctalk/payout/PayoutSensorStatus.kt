package cctalk.payout

enum class PayoutSensorStatus {
  /**
   * Status unknown.
   */
  Unknown,
  /**
   * Sensor not installed or not supported.
   */
  NotSupported,
  /**
   * Sensor is not triggered.
   */
  Untriggered,
  /**
   * Sensor is triggered.
   */
  Triggered;
}