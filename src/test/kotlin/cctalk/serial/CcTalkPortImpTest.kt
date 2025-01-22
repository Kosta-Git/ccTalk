package cctalk.serial

import arrow.core.Either
import be.inotek.communication.CcTalkChecksumTypes
import be.inotek.communication.packet.CcTalkPacket
import cctalk.CcTalkError
import cctalk.CcTalkStatus
import cctalk.serde.CcTalkSerializer
import cctalk.serde.CcTalkSerializationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class CcTalkPortImpTest {

  private lateinit var port: ConcurrentPort
  private lateinit var serializer: CcTalkSerializer
  private lateinit var ccTalkPort: CcTalkPortImp

  @BeforeEach
  fun setup() {
    port = mockk()
    serializer = mockk()
    ccTalkPort = CcTalkPortImp(port, serializer)
  }

  @Test
  fun `test talkCcNoResponse success`() = runBlocking {
    // Arrange
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(0u)
      checksumType(CcTalkChecksumTypes.Simple8)
    }
    val serializedData = ByteArray(5)
    val serializationResult = CcTalkSerializationResult(serializedData, 5)
    val responseData = ByteArray(5)
    val deserializedPacket = CcTalkPacket()

    coEvery { serializer.serialize(packet) } returns serializationResult
    coEvery { port.sendPacket(serializedData) } returns Either.Right(responseData)
    coEvery { serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8) } returns Either.Right(deserializedPacket)

    // Act
    val result = ccTalkPort.talkCcNoResponse(packet)

    // Assert
    assertEquals(CcTalkStatus.Ok, result.getOrNull()!!)
    coVerify {
      serializer.serialize(packet)
      port.sendPacket(serializedData)
      serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8)
    }
  }

  @Test
  fun `test talkCcNoResponse failure`() = runBlocking {
    // Arrange
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(0u)
      checksumType(CcTalkChecksumTypes.Simple8)
    }
    val serializedData = ByteArray(5)
    val serializationResult = CcTalkSerializationResult(serializedData, 5)

    coEvery { serializer.serialize(packet) } returns serializationResult
    coEvery { port.sendPacket(serializedData) } returns Either.Left(CcTalkError.TimeoutError())

    // Act
    val result = ccTalkPort.talkCcNoResponse(packet)

    // Assert
    assertEquals(CcTalkStatus.RcvTimeout, result.leftOrNull()?.status!!)
    coVerify {
      serializer.serialize(packet)
      port.sendPacket(serializedData)
    }
  }

  @Test
  fun `test talkCcLongResponse success`() = runBlocking {
    // Arrange
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(0u)
      data(ubyteArrayOf(1u, 2u, 3u))
      checksumType(CcTalkChecksumTypes.Simple8)
    }
    val serializedData = ByteArray(8)
    val serializationResult = CcTalkSerializationResult(serializedData, 8)
    val responseData = ByteArray(8)
    val deserializedPacket = CcTalkPacket.build {
      destination(1u)
      source(2u)
      header(0u)
      data(ubyteArrayOf(4u, 5u, 6u))
    }

    coEvery { serializer.serialize(packet) } returns serializationResult
    coEvery { port.sendPacket(serializedData) } returns Either.Right(responseData)
    coEvery { serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8) } returns Either.Right(deserializedPacket)

    // Act
    val result = ccTalkPort.talkCcLongResponse(packet)

    // Assert
    assertTrue(result.isRight())
    assertEquals(15L, result.getOrNull()) // 4 + 5 + 6 = 15
    coVerify {
      serializer.serialize(packet)
      port.sendPacket(serializedData)
      serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8)
    }
  }

  @Test
  fun `test talkCcStringResponse success`() = runBlocking {
    // Arrange
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(0u)
      data(ubyteArrayOf(72u, 101u, 108u, 108u, 111u)) // "Hello"
      checksumType(CcTalkChecksumTypes.Simple8)
    }
    val serializedData = ByteArray(10)
    val serializationResult = CcTalkSerializationResult(serializedData, 10)
    val responseData = ByteArray(10)
    val deserializedPacket = CcTalkPacket.build {
      destination(1u)
      source(2u)
      header(0u)
      data(ubyteArrayOf(72u, 101u, 108u, 108u, 111u)) // "Hello"
    }

    coEvery { serializer.serialize(packet) } returns serializationResult
    coEvery { port.sendPacket(serializedData) } returns Either.Right(responseData)
    coEvery { serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8) } returns Either.Right(deserializedPacket)

    // Act
    val result = ccTalkPort.talkCcStringResponse(packet)

    // Assert
    assertTrue(result.isRight())
    assertEquals("Hello", result.getOrNull())
    coVerify {
      serializer.serialize(packet)
      port.sendPacket(serializedData)
      serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8)
    }
  }

  @Test
  fun `test talkCc with custom transformation success`() = runBlocking {
    // Arrange
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(0u)
      data(ubyteArrayOf(1u, 2u, 3u))
      checksumType(CcTalkChecksumTypes.Simple8)
    }
    val serializedData = ByteArray(8)
    val serializationResult = CcTalkSerializationResult(serializedData, 8)
    val responseData = ByteArray(8)
    val deserializedPacket = CcTalkPacket.build {
      destination(1u)
      source(2u)
      header(0u)
      data(ubyteArrayOf(1u, 2u, 3u))
    }

    coEvery { serializer.serialize(packet) } returns serializationResult
    coEvery { port.sendPacket(serializedData) } returns Either.Right(responseData)
    coEvery { serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8) } returns Either.Right(deserializedPacket)

    // Act
    val result = ccTalkPort.talkCc(packet) { response ->
      response.data.size // Custom transformation
    }

    // Assert
    assertTrue(result.isRight())
    assertEquals(3, result.getOrNull())
    coVerify {
      serializer.serialize(packet)
      port.sendPacket(serializedData)
      serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8)
    }
  }

  @Test
  fun `test talkCc with deserialization failure`() = runBlocking {
    // Arrange
    val packet = CcTalkPacket.build {
      destination(2u)
      source(1u)
      header(0u)
      checksumType(CcTalkChecksumTypes.Simple8)
    }
    val serializedData = ByteArray(5)
    val serializationResult = CcTalkSerializationResult(serializedData, 5)
    val responseData = ByteArray(5)

    coEvery { serializer.serialize(packet) } returns serializationResult
    coEvery { port.sendPacket(serializedData) } returns Either.Right(responseData)
    coEvery { serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8) } returns Either.Left(CcTalkError.ChecksumError())

    // Act
    val result = ccTalkPort.talkCc(packet)

    // Assert
    assertTrue(result.isLeft())
    assertEquals(CcTalkStatus.ChSumErr, result.leftOrNull()?.status!!)
    coVerify {
      serializer.serialize(packet)
      port.sendPacket(serializedData)
      serializer.deserialize(responseData, CcTalkChecksumTypes.Simple8)
    }
  }
}