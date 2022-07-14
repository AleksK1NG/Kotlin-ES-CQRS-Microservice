package com.example.microservice.domain

import org.bson.codecs.pojo.annotations.BsonProperty
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal


@Document(collection = "bankAccounts")
open class BankAccountDocument(aggregateId: String? = null, email: String? = null, balance: BigDecimal? = null, currency: String? = null) {

    @BsonProperty(value = "_id")
    var id: String? = null

    @BsonProperty(value = "aggregateId")
    var aggregateId: String? = aggregateId

    @BsonProperty(value = "email")
    var email: String? = email

    @BsonProperty(value = "balance")
    var balance: BigDecimal? = balance

    @BsonProperty(value = "currency")
    var currency: String? = currency

//    constructor(aggregateId: String, email: String, balance: BigDecimal, currency: String): this() {
//        this.aggregateId = aggregateId
//        this.email = email
//        this.balance = balance
//        this.currency = currency
//    }

//
//        constructor(aggregateId: String, email: String, balance: BigDecimal, currency: String) {
//        this.aggregateId = aggregateId
//        this.email = email
//        this.balance = balance
//        this.currency = currency
//    }

    companion object {
        fun of(bankAccountAggregate: BankAccountAggregate): BankAccountDocument {
            return BankAccountDocument(
                bankAccountAggregate.aggregateId,
                bankAccountAggregate.email,
                bankAccountAggregate.balance,
                bankAccountAggregate.currency
            )
        }
    }


    override fun toString(): String {
        return "BankAccountDocument(id=$id, aggregateId=$aggregateId, email=$email, balance=$balance, currency=$currency)"
    }
}