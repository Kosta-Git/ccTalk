package cctalk.device

import arrow.core.Either
import arrow.core.raise.either
import cctalk.CcTalkCategory
import cctalk.CcTalkError
import kotlin.reflect.KProperty

data class DeviceInformation(
  val manufacturerId: String,
  val equipmentCategoryId: String,
  val equipmentCategory: CcTalkCategory,
  val productCode: String,
  val serialNumber: Long,
  val softwareVersion: String,
) {
  override fun toString(): String {
    return """
      |Manufacturer ID: $manufacturerId
      |Equipment Category ID: $equipmentCategoryId
      |Equipment Category: $equipmentCategory
      |Product Code: $productCode
      |Serial Number: $serialNumber
      |Software Version: $softwareVersion
    """.trimIndent()
  }
}

class CachedCcTalkProperty<T>(
  private val compute: suspend () -> Either<CcTalkError, T>
) {
  private var cached: T? = null

  operator fun getValue(thisRef: Any?, property: KProperty<*>): CachedCcTalk<T> =
    CachedCcTalk(compute)
}

fun <T> cachedCcTalkProp(compute: suspend () -> Either<CcTalkError, T>) = CachedCcTalkProperty(compute)

class CachedCcTalk<T>(
  private val compute: suspend () -> Either<CcTalkError, T>
) {
  private var cached: T? = null

  suspend fun get(forceRefresh: Boolean = false): Either<CcTalkError, T> = either {
    if(forceRefresh || cached == null)
      cached = compute().bind()

    if(cached == null)
      raise(CcTalkError.UnknownError("Cached value is null"))

    cached!!
  }
}
