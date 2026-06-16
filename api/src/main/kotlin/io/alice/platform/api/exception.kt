package io.alice.platform.api

open class AliceException : RuntimeException {
  constructor() : super()
  constructor(cause: Throwable?) : super(cause)
  constructor(message: String?) : super(message)
  constructor(message: String?, cause: Throwable?) : super(message, cause)
}

open class AliceRuntimeException : AliceException {
  constructor() : super()
  constructor(cause: Throwable?) : super(cause)
  constructor(message: String?) : super(message)
  constructor(message: String?, cause: Throwable?) : super(message, cause)
}
