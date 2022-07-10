package com.example.microservice.domain

import java.math.BigInteger

open class AccountAggregate(
    var email: String?,
    var version: BigInteger? = BigInteger.ZERO,
    var id: String?,
    var data: ByteArray? = null,
) {

    constructor(): this("", BigInteger.ONE, null)

    override fun toString(): String {
        return "AccountAggregate(email=$email, version=$version, id=$id, data=${data?.contentToString()})"
    }
}